#include <iostream>

#include <json/json.h>

#include "dataset.h"
#include "result.h"
#include "measurements.pb.h"
#include <boost/asio.hpp>
#include <boost/algorithm/string.hpp>
#include "avro/Encoder.hh"
#include "avro/Decoder.hh"
#include "measurements.avro.hh"

#include <avro/Generic.hh>
#include <avro/Specific.hh>
using namespace std;
using boost::asio::ip::tcp;
using namespace avro;

void processJSON(tcp::iostream& stream){
    Json::Value val;
    Json::Reader reader;

    std::vector<Dataset> datasets;
    std::vector<Result> results;

    /* Read json string from the stream */
    string s;
    getline(stream, s, '\0');

    /* Parse string */
    reader.parse(s, val);

    datasets.clear();
    results.clear();
    for (int i = 0; i < val.size(); i++) {
        datasets.emplace_back();
        datasets[i].Deserialize(val[i]);
        /* Calculate averages */
        results.emplace_back(datasets[i].getInfo(), datasets[i].getRecords());
    }

    /* Create output JSON structure */
    Json::Value out;
//    Json::FastWriter writer;
    Json::StyledWriter writer;
    for (int i = 0; i < results.size(); i++) {
        Json::Value result;
        results[i].Serialize(result);
        out[i] = result;
    }

    /* Send the result back */
    std::string output = writer.write(out);
    stream << output;
    cout << output;
}
int32_t getMsgSize(tcp::iostream& stream) {
     // Read the four bytes of the integer value in network byte order
    int32_t value = 0;
    uint8_t byte;
    for (int i = 0; i < 4; i++) {
        if (!stream.read((char*)&byte, 1)) {
            // Handle error
            throw std::runtime_error("Error reading int32_t value from stream");
        }
        value |= ((int32_t)byte << (8 * i));
    }

    // Convert the integer value to host byte order
    return ntohl(value);
}
void processAvro(tcp::iostream& stream){
    int32_t messageSize = getMsgSize(stream);
    char *buffer = new char[messageSize];
    // Read msg to buffer
    stream.read(buffer, messageSize);
    std::unique_ptr<avro::InputStream> in = avro::memoryInputStream((const uint8_t*) buffer, messageSize);
    // Create a binary decoder
    avro::DecoderPtr decoder = avro::binaryDecoder();

    // Set the input stream for the decoder
    decoder->init(*in);

    // Deserialize the Avro message into a GenericDatum
    esw_avro::ADatasets receivedDatasets;
    esw_avro::AResults results;
    avro::decode(*decoder, receivedDatasets);
    // Compute results
    for (auto dataset: receivedDatasets.datasets){
        esw_avro::AResult dataset_result;
        dataset_result.info = dataset.info;
        for(auto record: dataset.records){
            double avg = 0;
            for(auto measured_value: record.second){
                avg += measured_value;
            }
        
            esw_avro::AAverage value_average;
            value_average.dtype.datatype = record.first;
            value_average.average = avg / record.second.size();

            // push to dataset result
            dataset_result.averages.push_back(value_average);
        }
        // push to results
        results.result.push_back(dataset_result);
     
    }
    // send results back
    avro::OutputStreamPtr out = avro::ostreamOutputStream(stream);
    avro::EncoderPtr encoder = avro::binaryEncoder();
    encoder->init(*out);
    avro::encode(*encoder, results);
    encoder->flush();
 
}

void processProtobuf(tcp::iostream& stream){
    
    int32_t messageSize = getMsgSize(stream);
    char *buffer = new char[messageSize];
    // Read msg to buffer
    stream.read(buffer, messageSize);
    esw::PDatasets receivedDatasets;
    // Parse from buffer
    if (!receivedDatasets.ParseFromArray(buffer, messageSize)) {
        throw std::logic_error("MESSAGE was not parsed!");

    }
    esw::PResults results;
    // Compute results
    for (auto dataset : receivedDatasets.dataset()){
            esw::PResult datasetResult;
            datasetResult.mutable_info()->CopyFrom(dataset.info());
            std::map <esw::PDataType, std::vector<double>> recordsByDataType;
            // Convert to map
            for(const auto &record: dataset.records()){
                recordsByDataType[record.datatype()].push_back(record.measured_value());

            }
            // Compute per data type
            for (const auto& datatypePair : recordsByDataType){
                esw::PDataType recordDataType = datatypePair.first;
                esw::PAverage datatype_avg;
                datatype_avg.set_datatype(recordDataType);
                double average = 0;
                for (const auto &record: datatypePair.second){
                    average += record;
                }
                datatype_avg.set_average(average/datatypePair.second.size());
                datasetResult.add_averages()->CopyFrom(datatype_avg);
            }


            results.add_result()->CopyFrom(datasetResult);
            
    }
    // Send output
    results.SerializeToOstream(&stream);
}

int main(int argc, char *argv[]) {

    if (argc != 3) {
        cout << "Error: two arguments required - ./server  <port> <protocol>" << endl;
        return 1;
    }

    // unsigned short int port = 12345;
    unsigned short int port = atoi(argv[1]);

    // std::string protocol = "json";
    std::string protocol(argv[2]);
    boost::to_upper(protocol);
    try {
        boost::asio::io_service io_service;

        tcp::endpoint endpoint(tcp::v4(), port);
        tcp::acceptor acceptor(io_service, endpoint);

        while (true) {
            cout << "Waiting for message in " + protocol + " format..." << endl;
            tcp::iostream stream;
            boost::system::error_code ec;
            acceptor.accept(*stream.rdbuf(), ec);

            if(protocol == "JSON"){
                processJSON(stream);
            }else if(protocol == "AVRO"){
                processAvro(stream);
            }else if(protocol == "PROTO"){
                processProtobuf(stream);
            }else{
                throw std::logic_error("Protocol not yet implemented");
            }

        }

    }
    catch (std::exception &e) {
        std::cerr << e.what() << std::endl;
    }

    return 0;
}
