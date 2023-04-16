package cz.esw.serialization.handler;

import cz.esw.serialization.ResultConsumer;
import cz.esw.serialization.avro.*;
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
        ADataset dataset = new ADataset();
        dataset.setInfo(new AMeasurementInfo(datasetId, timestamp, measurerName));

        Map<String, List<Double>> records = new HashMap<>();
        ADataType.getClassSchema().getEnumSymbols().forEach(e -> records.put(e, new LinkedList<>()));
        dataset.setRecords(records);

        datasets.put(datasetId, dataset);
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
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(new ByteArrayOutputStream(), null);
        DatumWriter<ADatasets> writer = new SpecificDatumWriter<>(ADatasets.class);
        writer.write(data, encoder);
        encoder.flush();

        // write msg size
        int msgSize = new ByteArrayOutputStream().size();
        DataOutputStream out = new DataOutputStream(outputStream);
        out.writeInt(msgSize);
        out.flush();

        // process input
        final BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(inputStream, null);
        AResults input = new SpecificDatumReader<>(AResults.class).read(null, binaryDecoder);

        //  send results
        input.getResult().forEach(result -> {
                    AMeasurementInfo info = result.getInfo();
                    consumer.acceptMeasurementInfo(info.getId(), info.getTimestamp(), info.getMeasurerName());
                    result.getAverages().forEach(e ->
                            consumer.acceptResult(DataType.getDataType(Integer.parseInt(e.toString())), e.getAverage()));
                }
        );

        inputStream.close();
        outputStream.close();
    }

}
