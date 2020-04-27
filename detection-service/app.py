import json;
import base64;
from flask import Flask, request
from flask.json import jsonify
from kafka import KafkaProducer, KafkaConsumer, TopicPartition

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
    # Produce a new record in detection-queries
    body = request.get_json()
    correlationId = json.dumps(body).encode('base64')
    producer = KafkaProducer(bootstrap_servers='broker-0:9092', value_serializer=lambda v: json.dumps(v).encode('utf-8'))
    future = producer.send(DETECTION_QUERIES_TOPIC, body, correlationId)

    # Consume a record in detection-responses
    consumer = KafkaConsumer(bootstrap_servers='broker-0:9092', group_id="detection-service", session_timeout_ms=6000, request_timeout_ms=6010)
    consumer.subscribe(topics=[DETECTION_RESPONSES_TOPIC])
    for msg in consumer:
        if msg.key == correlationId:
            consumer.close()
            return jsonify(msg.value)
    return []

@app.route('/scan/<domain>')
def getDomainScan(domain):
    return []

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=80, debug=True)