package cz.esw.serialization.handler;

import cz.esw.serialization.ResultConsumer;
import cz.esw.serialization.json.DataType;
import cz.esw.serialization.proto.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
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
        PMeasurementInfo.Builder newDataset = PMeasurementInfo.newBuilder()
                .setId(datasetId)
                .setTimestamp(timestamp)
                .setMeasurerName(measurerName);

        PDataset.Builder datasetBuilder = PDataset.newBuilder();
        datasetBuilder.setInfo(newDataset);

        datasets.put(datasetId, datasetBuilder.build());
    }

    @Override
    public void handleValue(int datasetId, DataType type, double value) {
        PDataset dataset = datasets.get(datasetId);
        if (dataset == null) {
            throw new IllegalArgumentException("Dataset with id " + datasetId + " not initialized.");
        }

        PRecord pRecord = dataset.getRecords(type.getType());

        datasets.put(datasetId, dataset.toBuilder().addRecords(type.getType(), pRecord).build());
    }

    @Override
    public void getResults(ResultConsumer consumer) throws IOException {
//     TODO   Process values obtained so far and pass the results to the {@code consumer}.

        PDatasets message = PDatasets.newBuilder().putAllDatasets(datasets).build();

        message.writeTo(outputStream);

        PResults res = PResults.parseFrom(inputStream);

        for (PResult result : res.getResultList()) {
            PMeasurementInfo info = result.getInfo();
            consumer.acceptMeasurementInfo(info.getId(), info.getTimestamp(), info.getMeasurerName());
            List<PAverage> averages = result.getAveragesList();
            var downAm = averages.get(DataType.DOWNLOAD.getType());
//averages: [[type]: amount,...] ???
//            consumer.acceptResult(DataType.DOWNLOAD, downAm);
//            consumer.acceptResult(DataType.UPLOAD, );
//            consumer.acceptResult(DataType.PING, );
        }
    }
}