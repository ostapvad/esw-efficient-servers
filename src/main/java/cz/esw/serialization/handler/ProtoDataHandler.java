package cz.esw.serialization.handler;

import cz.esw.serialization.ResultConsumer;
import cz.esw.serialization.json.DataType;
import cz.esw.serialization.proto.*;

import java.io.DataOutputStream;
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
        datasets.put(datasetId, dataset.toBuilder().addRecords(
                PRecord.newBuilder().setDatatype(PDataType.forNumber(type.getNumber())).setMeasuredValue(value)).build());
    }

    @Override
    public void getResults(ResultConsumer consumer) throws IOException {
        // init data
        PDatasets data = PDatasets.newBuilder().addAllDataset(this.datasets.values()).build();

        // write msg size
        DataOutputStream dos = new DataOutputStream(outputStream);
        dos.writeInt(data.getSerializedSize());
        dos.flush();
        data.writeTo(outputStream);

        // process input
        PResults input = PResults.parseFrom(inputStream);
        List<PResult> tmp = input.getResultList();
        tmp.forEach(result -> {
            PMeasurementInfo info = result.getInfo();
            consumer.acceptMeasurementInfo(info.getId(), info.getTimestamp(), info.getMeasurerName());
            result.getAveragesList().forEach(e ->
                    consumer.acceptResult(DataType.getDataType(e.getDatatypeValue()), e.getAverage()));
        });

        inputStream.close();
        outputStream.close();
    }
}
