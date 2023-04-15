#include <iostream>

#include <json/json.h>

#include "dataset.h"
#include "result.h"
#include "measurements.pb.h"
#include <boost/asio.hpp>
#include <boost/algorithm/string.hpp>

using namespace std;
using boost::asio::ip::tcp;

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

void processAvro(tcp::iostream& stream){
    throw std::logic_error("TODO: Implement avro");
}
int32_t read_int32(tcp::iostream& stream) {
    int32_t value = 0;
    uint8_t byte;

    // Read the four bytes of the integer value in network byte order
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
void processProtobuf(tcp::iostream& stream){
    // Nepravilno jobanije volki
    printf("Allo\n");
    string s;
    int32_t messageSize = read_int32(stream);
    char *buffer = new char[messageSize];
    stream.read(buffer, messageSize);
    //std::cout << value << std::endl;
    //getline(stream, s, '\0');
    esw::PDatasets receivedDatasets;

   // std::cout << s << std::endl;
    //esw::PDatasets receiverd_datasets;

    if (!receivedDatasets.ParseFromArray(buffer, messageSize)) {
        std::cerr << "Padlo\n";

    }
    for (auto dataset : receivedDatasets.dataset()){
            std::cout << "Measurement Info: " << dataset.info().measurername() << std::endl;
    }

   
    std::cout<< s << std::endl;
  // Handle parsing error
//   std::cerr << "Error parsing input string" << std::endl;
//   return;
// }



    // int messageSize = readAndDecodeMessageSize(stream)
    // int message_size = tcp::iostream::read_int32(stream);
    
    throw std::logic_error("TODO: Implement protobuf");
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
