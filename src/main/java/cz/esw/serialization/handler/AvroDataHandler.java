package cz.esw.serialization.handler;

import cz.esw.serialization.ResultConsumer;
import cz.esw.serialization.avro.ADataset;
import cz.esw.serialization.avro.ADatasets;
import cz.esw.serialization.avro.AMeasurementInfo;
import cz.esw.serialization.avro.AResults;
import cz.esw.serialization.json.DataType;
import org.apache.avro.io.*;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;

import java.io.*;
import java.util.*;

/**
 * @author Marek Cuchý (CVUT)
 */
public class AvroDataHandler implements DataHandler {

    private final InputStream inputStream;
    private final OutputStream outputStream;

    protected Map<Integer, ADataset> datasets;


    public AvroDataHandler(InputStream is, OutputStream os) {
        this.inputStream = is;
        this.outputStream = os;
    }

    @Override
    public void initialize() {
        datasets = new HashMap<>();
    }

    @Override
    public void handleNewDataset(int datasetId, long timestamp, String measurerName) {
        Map<String, List<Double>> records = new HashMap<>();
        Arrays.stream(DataType.values()).toList().forEach(e -> records.put(e.toString(), new LinkedList<>()));
        datasets.put(datasetId, new ADataset(new AMeasurementInfo(datasetId, timestamp, measurerName), records));
    }

    @Override
    public void handleValue(int datasetId, DataType type, double value) {
        ADataset aDataset = datasets.get(datasetId);
        if (aDataset == null) {
            throw new IllegalArgumentException("Dataset with id " + datasetId + " not initialized.");
        }
        aDataset.getRecords().computeIfAbsent(type.toString(), t -> new ArrayList<>()).add(value);
    }

    @Override
    public void getResults(ResultConsumer consumer) throws IOException {
        // init data
        ADatasets data = new ADatasets();
        data.setDatasets(new ArrayList<>(datasets.values()));

        // write measurements
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(byteArrayOutputStream, null);
        DatumWriter<ADatasets> writer = new SpecificDatumWriter<>(ADatasets.class);
        writer.write(data, encoder);
        encoder.flush();

        // write msg size
        int msgSize = byteArrayOutputStream.size();
        DataOutputStream out = new DataOutputStream(outputStream);
        out.writeInt(msgSize);
        out.flush();
        byteArrayOutputStream.writeTo(out);

        // process input
        final BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(inputStream, null);
        AResults input = new SpecificDatumReader<>(AResults.class).read(null, binaryDecoder);

        //  send results
        input.getResult().forEach(result -> {
                    AMeasurementInfo info = result.getInfo();
                    consumer.acceptMeasurementInfo(info.getId(), info.getTimestamp(), info.getMeasurerName());
                    result.getAverages().forEach(e ->
                            consumer.acceptResult(DataType.valueOf(e.getDtype().getDatatype()), e.getAverage())
                    );
                }
        );
        inputStream.close();
        outputStream.close();
    }

}
