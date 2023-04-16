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
        List<ARecord> records = new ArrayList<>();
        for (var v : ADataType.values()) {
            var res = ARecord.newBuilder()
                    .setDatatype(ADataType.valueOf(String.valueOf(v)))
                    .setValues(Collections.emptyList())
                    .build();
            records.add(res);
        }

        datasets.put(datasetId,
                ADataset.newBuilder().setInfo(new AMeasurementInfo(datasetId, timestamp, measurerName))
                        .setRecords(records).build());

//        System.out.println("ssss");
    }

    @Override
    public void handleValue(int datasetId, DataType type, double value) {
        ADataset dataset = datasets.get(datasetId);
        if (dataset == null) {
            throw new IllegalArgumentException("Dataset with id " + datasetId + " not initialized.");
        }
        var a = ARecord.newBuilder().
                setDatatype(ADataType.valueOf(type.toString())).setValues(Collections.singletonList(value)).build();
        List<ARecord> recs = dataset.getRecords();
        recs.add(a);
        dataset.setRecords(recs);
        datasets.put(datasetId, dataset);
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
        AResults incomingMessage = new AResults();
        DatumReader<AResults> datumReader = new SpecificDatumReader<>(AResults.class);
        BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(inputStream, null);
        datumReader.read(incomingMessage, binaryDecoder);
        AResults input = new SpecificDatumReader<>(AResults.class).read(null, binaryDecoder);

        //  send results
        input.getResult().forEach(result -> {
                    AMeasurementInfo info = result.getMeasurementInfo();
                    //  TODO ya hz jestli tut ne budet problemy s charSeq
                    consumer.acceptMeasurementInfo(info.getId(), info.getTimestamp(), info.getMeasurerName());
                    result.getAverages().forEach(e ->
                            consumer.acceptResult(DataType.getDataType(Integer.parseInt(e.toString())), e.getAverage()));
                }
        );

        inputStream.close();
        outputStream.close();
    }

}
