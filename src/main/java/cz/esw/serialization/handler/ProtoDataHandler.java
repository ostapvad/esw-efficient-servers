package cz.esw.serialization.handler;

import cz.esw.serialization.ResultConsumer;
import cz.esw.serialization.json.DataType;
import cz.esw.serialization.proto.PDataset;
import cz.esw.serialization.proto.PMeasurementInfo;
import cz.esw.serialization.proto.PRecord;
import cz.esw.serialization.proto.PResults;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Marek Cuchý (CVUT)
 */
public class ProtoDataHandler implements DataHandler {
    private final InputStream inputStream;
    private final OutputStream outputStream;
    protected Map<Integer, PDataset> datasets;


    public ProtoDataHandler(InputStream is, OutputStream os) {
        this.inputStream = is;
        this.outputStream = os;
    }

    @Override
    public void initialize() {
        datasets = new HashMap<>();
    }

    @Override
    public void handleNewDataset(int datasetId, long timestamp, String measurerName) {
        var info = PMeasurementInfo.newBuilder()
                .setId(datasetId)
                .setTimestamp(timestamp)
                .setMeasurerName(measurerName);

        datasets.put(datasetId, PDataset.newBuilder().setInfo(info).build());
    }

    @Override
    public void handleValue(int datasetId, DataType type, double value) {
        PDataset dataset = datasets.get(datasetId);
        if (dataset == null) {
            throw new IllegalArgumentException("Dataset with id " + datasetId + " not initialized.");
        }

        PRecord pRecord = dataset.getRecords(type.getNumber());

        datasets.put(datasetId, dataset.toBuilder().addRecords(type.getNumber(), pRecord).build());
    }

    @Override
    public void getResults(ResultConsumer consumer) throws IOException {
//        AtomicInteger msgSize = new AtomicInteger();
        datasets.forEach((a, dataset) -> {
            try {
                dataset.writeDelimitedTo(outputStream);
//                msgSize.addAndGet(dataset.getSerializedSize());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
//        new DataOutputStream(outputStream).writeInt(msgSize.intValue());


        PResults input = PResults.parseFrom(inputStream);

        input.getResultList().forEach(result -> {
            PMeasurementInfo info = result.getInfo();
            consumer.acceptMeasurementInfo(info.getId(), info.getTimestamp(), info.getMeasurerName());
            result.getAveragesList().forEach(e ->
                    consumer.acceptResult(DataType.getDataType(Integer.parseInt(e.toString())), e.getAverage()));
        });


    }
}


