package cz.esw.serialization.handler;

import com.google.protobuf.Descriptors;
import cz.esw.serialization.ResultConsumer;
import cz.esw.serialization.json.DataType;
import cz.esw.serialization.proto.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

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

        List<PRecord> records = new ArrayList<>();
        for(Descriptors.EnumValueDescriptor padlo: PDataType.getDescriptor().getValues()){
            PRecord record = PRecord.newBuilder()
                    .setDatatype(PDataType.valueOf(padlo))
                    .addAllValues(Collections.emptyList())
                    .build();
            records.add(record);

        }


        datasets.put(datasetId, PDataset.newBuilder().setInfo(info).addAllRecords(records).build());
    }

    @Override
    public void handleValue(int datasetId, DataType type, double value) {
        PDataset dataset = datasets.get(datasetId);
        if (dataset == null) {
            throw new IllegalArgumentException("Dataset with id " + datasetId + " not initialized.");
        }

        PRecord pRecord = dataset.getRecords(type.getNumber());
        System.out.println("gnida");
        datasets.put(datasetId, dataset.toBuilder().addRecords(type.getNumber(), pRecord).build());
    }

    @Override
    public void getResults(ResultConsumer consumer) throws IOException {
        PDatasets datasetToSend = PDatasets.newBuilder().addAllDataset(datasets.values()).build();
        for (PDataset dataset : datasetToSend.getDatasetList()){
            System.out.println(dataset.getInfo().getMeasurerName());
        }
        datasetToSend.writeTo(this.outputStream);
        this.outputStream.write(0);
        this.outputStream.flush();
//
//        //        AtomicInteger msgSize = new AtomicInteger();
//        System.out.println("mraz");
//        datasets.forEach((a, dataset) -> {
//            try {
//                dataset.writeDelimitedTo(outputStream);
//                System.out.println("TVAR");
////                msgSize.addAndGet(dataset.getSerializedSize());
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        });
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


