package cz.esw.serialization.handler;

import com.google.protobuf.Descriptors;
import cz.esw.serialization.ResultConsumer;
import cz.esw.serialization.json.DataType;
import cz.esw.serialization.proto.*;

import java.io.DataOutputStream;
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
//
//        List<PRecord> records = new ArrayList<>();
//        for (Descriptors.EnumValueDescriptor padlo : PDataType.getDescriptor().getValues()) {
//            PRecord record = PRecord.newBuilder()
//                    .setDatatype(PDataType.valueOf(padlo))
//                    .addAllValues(Collections.emptyList())
//                    .build();
//            records.add(record);
//
//        }


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
//        PRecord pRecord =  PRecord.newBuilder().setDatatypeValue(type.getNumber()).set.setValues(type.getNumber(), value).build();//dataset.getRecords(type.getNumber());
//        System.out.println("gnida");
//        datasets.put(datasetId, dataset.toBuilder().addRecords(pRecord).build());
    }

    @Override
    public void getResults(ResultConsumer consumer) throws IOException {
        PDatasets data = PDatasets.newBuilder().addAllDataset(this.datasets.values()).build();
//        for(PDataset dataset: data.getDatasetList()){
//            System.out.println(dataset.toString());
//        }
//        for(PDataset dataset: data.getDatasetList()){
//            for(PRecord gnida: dataset.getRecordsList()){
//                if(gnida.getDatatypeValue() == PDataType.DOWNLOAD_VALUE){
//                    System.out.println("daaads\n");
//                }
//            }
//        }
        DataOutputStream dos = new DataOutputStream(outputStream);
        dos.writeInt(data.getSerializedSize());
        dos.flush();
        data.writeTo(outputStream);

        PResults input = PResults.parseFrom(inputStream);
        List<PResult>tmp = input.getResultList();
//        for(PResult entry: tmp){
//            System.out.println(entry.getInfo());
//            System.out.println(entry.getAveragesList());
//        }
        tmp.forEach(result -> {
            PMeasurementInfo info = result.getInfo();
            consumer.acceptMeasurementInfo(info.getId(), info.getTimestamp(), info.getMeasurerName());
            result.getAveragesList().forEach(e ->
                    consumer.acceptResult(DataType.getDataType(e.getDatatypeValue()), e.getAverage()));
        });


    }
}
//Waiting for message in JSON format...
//        [
//        {
//        "averages" : {
//        "DOWNLOAD" : 95469.0,
//        "PING" : 386.0,
//        "UPLOAD" : 8138.0
//        },
//        "info" : {
//        "id" : 1569741360,
//        "measurerName" : "kristyna",
//        "timestamp" : 1681607042730
//        }
//        },
//        {
//        "averages" : {
//        "DOWNLOAD" : 77669.5,
//        "PING" : 658.5,
//        "UPLOAD" : 5886.0
//        },
//        "info" : {
//        "id" : 1785505948,
//        "measurerName" : "jirka",
//        "timestamp" : 1681607042731
//        }
//        }
//        ]
//        Waiting for message in JSON format...
//        [
//        {
//        "averages" : {
//        "DOWNLOAD" : 36863.0,
//        "PING" : 468.5,
//        "UPLOAD" : 7313.5
//        },
//        "info" : {
//        "id" : 49567875,
//        "measurerName" : "jana",
//        "timestamp" : 1681607042881
//        }
//        },
//        {
//        "averages" : {
//        "DOWNLOAD" : 80922.0,
//        "PING" : 770.0,
//        "UPLOAD" : 5545.0
//        },
//        "info" : {
//        "id" : 1888030084,
//        "measurerName" : "jirka",
//        "timestamp" : 1681607042881
//        }
//        }
//        ]
//        Waiting for message in JSON format...
//        [
//        {
//        "averages" : {
//        "DOWNLOAD" : 76900.0,
//        "PING" : 806.5,
//        "UPLOAD" : 5380.0
//        },
//        "info" : {
//        "id" : 224392037,
//        "measurerName" : "jana",
//        "timestamp" : 1681607042883
//        }
//        },
//        {
//        "averages" : {
//        "DOWNLOAD" : 59123.0,
//        "PING" : 267.0,
//        "UPLOAD" : 5902.0
//        },
//        "info" : {
//        "id" : 543062387,
//        "measurerName" : "jana",
//        "timestamp" : 1681607042883
//        }
//        }
//        ]
//        Waiting for message in JSON format...
//        [
//        {
//        "averages" : {
//        "DOWNLOAD" : 94042.0,
//        "PING" : 627.0,
//        "UPLOAD" : 4516.0
//        },
//        "info" : {
//        "id" : 1755132112,
//        "measurerName" : "jirka",
//        "timestamp" : 1681607042884
//        }
//        },
//        {
//        "averages" : {
//        "DOWNLOAD" : 62965.5,
//        "PING" : 322.5,
//        "UPLOAD" : 5426.0
//        },
//        "info" : {
//        "id" : 27502062,
//        "measurerName" : "jana",
//        "timestamp" : 1681607042884
//        }
//        }
//        ]
//        Waiting for message in JSON format...
//        [
//        {
//        "averages" : {
//        "DOWNLOAD" : 12228.0,
//        "PING" : 287.5,
//        "UPLOAD" : 3215.0
//        },
//        "info" : {
//        "id" : 1535517408,
//        "measurerName" : "kristyna",
//        "timestamp" : 1681607042885
//        }
//        },
//        {
//        "averages" : {
//        "DOWNLOAD" : 67694.0,
//        "PING" : 258.5,
//        "UPLOAD" : 4357.5
//        },
//        "info" : {
//        "id" : 138026517,
//        "measurerName" : "jana",
//        "timestamp" : 1681607042886
//        }
//        }
//        ]
//        Waiting for message in JSON format...
//        [
//        {
//        "averages" : {
//        "DOWNLOAD" : 90368.5,
//        "PING" : 695.5,
//        "UPLOAD" : 6148.0
//        },
//        "info" : {
//        "id" : 1598713773,
//        "measurerName" : "kristyna",
//        "timestamp" : 1681607042887
//        }
//        },
//        {
//        "averages" : {
//        "DOWNLOAD" : 47729.0,
//        "PING" : 526.5,
//        "UPLOAD" : 7074.0
//        },
//        "info" : {
//        "id" : 2086381267,
//        "measurerName" : "jirka",
//        "timestamp" : 1681607042887
//        }
//        }
//        ]
//        Waiting for message in JSON format...
//        [
//        {
//        "averages" : {
//        "DOWNLOAD" : 66147.0,
//        "PING" : 706.5,
//        "UPLOAD" : 6013.5
//        },
//        "info" : {
//        "id" : 2036211222,
//        "measurerName" : "jirka",
//        "timestamp" : 1681607042888
//        }
//        },
//        {
//        "averages" : {
//        "DOWNLOAD" : 50345.0,
//        "PING" : 616.0,
//        "UPLOAD" : 4594.5
//        },
//        "info" : {
//        "id" : 1656381957,
//        "measurerName" : "jana",
//        "timestamp" : 1681607042888
//        }
//        }
//        ]
//        Waiting for message in JSON format...
//        [
//        {
//        "averages" : {
//        "DOWNLOAD" : 57867.0,
//        "PING" : 874.0,
//        "UPLOAD" : 2424.5
//        },
//        "info" : {
//        "id" : 1997032021,
//        "measurerName" : "kristyna",
//        "timestamp" : 1681607042889
//        }
//        },
//        {
//        "averages" : {
//        "DOWNLOAD" : 32923.5,
//        "PING" : 456.0,
//        "UPLOAD" : 9218.5
//        },
//        "info" : {
//        "id" : 1688013298,
//        "measurerName" : "jirka",
//        "timestamp" : 1681607042889
//        }
//        }
//        ]
//        Waiting for message in JSON format...
//        [
//        {
//        "averages" : {
//        "DOWNLOAD" : 59102.5,
//        "PING" : 370.0,
//        "UPLOAD" : 4265.0
//        },
//        "info" : {
//        "id" : 688413450,
//        "measurerName" : "jana",
//        "timestamp" : 1681607042890
//        }
//        },
//        {
//        "averages" : {
//        "DOWNLOAD" : 61447.0,
//        "PING" : 285.0,
//        "UPLOAD" : 6379.5
//        },
//        "info" : {
//        "id" : 947956792,
//        "measurerName" : "petr",
//        "timestamp" : 1681607042890
//        }
//        }
//        ]
//        Waiting for message in JSON format...
//        [
//        {
//        "averages" : {
//        "DOWNLOAD" : 42428.5,
//        "PING" : 727.0,
//        "UPLOAD" : 3214.0
//        },
//        "info" : {
//        "id" : 153810577,
//        "measurerName" : "kristyna",
//        "timestamp" : 1681607042891
//        }
//        },
//        {
//        "averages" : {
//        "DOWNLOAD" : 53658.0,
//        "PING" : 345.5,
//        "UPLOAD" : 6959.0
//        },
//        "info" : {
//        "id" : 6726778,
//        "measurerName" : "petr",
//        "timestamp" : 1681607042891
//        }
//        }
//        ]