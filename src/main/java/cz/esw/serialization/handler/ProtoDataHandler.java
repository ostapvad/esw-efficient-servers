package cz.esw.serialization.handler;

import cz.esw.serialization.ResultConsumer;
import cz.esw.serialization.json.DataType;
import cz.esw.serialization.proto.PDataset;
import cz.esw.serialization.proto.PMeasurementInfo;

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
//        TODO call handleNewDataset(...)?

	}

	@Override
	public void getResults(ResultConsumer consumer) {
//     TODO   Process values obtained so far and pass the results to the {@code consumer}.

	}
}