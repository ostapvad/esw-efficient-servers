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
        //TODO change to ENUM
        aDataset.getRecords().computeIfAbsent(type.toString(), t -> new ArrayList<>()).add(value);
    }

    @Override
    public void getResults(ResultConsumer consumer) throws IOException {
        // init data
        ADatasets data = new ADatasets();
        data.setDatasets(new ArrayList<>(datasets.values()));
        for (ADataset gnida : datasets.values()) {
            System.out.println(gnida.getInfo());
            System.out.println(gnida.getRecords());
        }

        // write measurements
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(byteArrayOutputStream, null);
        DatumWriter<ADatasets> writer = new SpecificDatumWriter<>(ADatasets.class);
        writer.write(data, encoder);
        encoder.flush();

        // write msg size
        int msgSize = byteArrayOutputStream.size();
        System.out.println(msgSize);
        DataOutputStream out = new DataOutputStream(outputStream);
        /// Change
        out.writeInt(msgSize);
        out.flush();
        byteArrayOutputStream.writeTo(out);

        // process input
        final BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(inputStream, null);
        AResults input = new SpecificDatumReader<>(AResults.class).read(null, binaryDecoder);
        for (AResult result : input.getResult()) {
            System.out.println(result.getInfo());
            System.out.println(result.getAverages());
        }

        //  send results
        input.getResult().forEach(result -> {
                    AMeasurementInfo info = result.getInfo();
                    consumer.acceptMeasurementInfo(info.getId(), info.getTimestamp(), info.getMeasurerName());
                    result.getAverages().forEach(e ->
                    {
                       // var d = DataType.getDataType(   (e.getDtype().getDatatype()));
                        consumer.acceptResult(DataType.valueOf(e.getDtype().getDatatype()), e.getAverage());
                    });
                }
        );
        inputStream.close();
        outputStream.close();
    }

}
