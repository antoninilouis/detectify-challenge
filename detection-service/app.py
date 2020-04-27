import json;
import base64;
from flask import Flask, request
from flask.json import jsonify
from kafka import KafkaProducer, KafkaConsumer

DETECTION_QUERIES_TOPIC = "detection-queries"
DETECTION_RESPONSES_TOPIC = "detection-responses"

app = Flask(__name__)

# Asynchronous Request-Response pattern
# CorrelationID is provided as record key
# CorrelationID is the Base64 encoding of the record value
# The produced record value is the domain scan list
# The consumed record value is the domains using Nginx
@app.route('/scan', methods=['POST'])
def scan():
    body = request.get_json()

    # Produce a new record in detection-queries
    producer = KafkaProducer(bootstrap_servers='broker-0:9092', value_serializer=lambda v: json.dumps(v).encode('utf-8'))
    future = producer.send(DETECTION_QUERIES_TOPIC, body, json.dumps(body).encode('base64'))
    future.get(timeout=5)

    # Consume a record in detection-responses
    consumer = KafkaConsumer(DETECTION_RESPONSES_TOPIC, bootstrap_servers='broker-0:9092')
    for msg in consumer:
        print(msg)
    return jsonify(body)

@app.route('/scan/<domain>')
def getDomainScan(domain):
    return {}

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=80, debug=True)