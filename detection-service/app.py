import os
import json;
import base64;
from flask import Flask, request, g
from flask.json import jsonify
from kafka import KafkaProducer, KafkaConsumer, TopicPartition
from flasgger import Swagger
import psycopg2

DETECTION_QUERIES_TOPIC = "detection-queries"
DETECTION_RESPONSES_TOPIC = "detection-responses"

app = Flask(__name__)
swagger = Swagger(app)

app.config.update(dict(
    POSTGRES_HOST=os.environ.get('POSTGRES_HOST'),
    POSTGRES_DB=os.environ.get('POSTGRES_DB'),
    POSTGRES_USER=os.environ.get('POSTGRES_USER'),
    POSTGRES_PASSWORD=os.environ.get('POSTGRES_PASSWORD'),
    BROKER_HOST=os.environ.get('BROKER_HOST'),
    BROKER_PORT=os.environ.get('BROKER_PORT')
))

# Asynchronous Request-Response pattern
# CorrelationID is provided as record key
# CorrelationID is the Base64 encoding of the record value
# The produced record value is the domain scan list
# The consumed record value is the domains using Nginx
@app.route('/scan', methods=['POST'])
def scan():
    """
    file: swagger/scan.yml
    """
    # Produce a new record in detection-queries
    body = request.get_json()
    correlationId = json.dumps(body).encode('base64')
    producer = KafkaProducer(bootstrap_servers=app.config['BROKER_HOST'] + ':' + app.config['BROKER_PORT'], value_serializer=lambda v: json.dumps(v).encode('utf-8'))
    future = producer.send(DETECTION_QUERIES_TOPIC, body, correlationId)

    # Consume a record in detection-responses
    consumer = KafkaConsumer(bootstrap_servers=app.config['BROKER_HOST'] + ':' + app.config['BROKER_PORT'], value_deserializer=lambda v: json.loads(v),
        group_id="detection-service", session_timeout_ms=6000, 
        request_timeout_ms=6010, consumer_timeout_ms=5000)
    consumer.subscribe(topics=[DETECTION_RESPONSES_TOPIC])

    try:
        for msg in consumer:
            if msg.key == correlationId:
                consumer.close()
                print(msg.value)
                return jsonify(msg.value)
    except StopIteration:
        return jsonify([])

@app.route('/scan/<hostname>')
def getDomainScan(hostname):
    """
    file: swagger/scan-host.yml
    """
    db = get_db()
    cur = db.cursor()
    cur.execute("""
        SELECT DISTINCT public."ServerScan".technology FROM public."ServerScan" INNER JOIN public."HttpServer" 
        ON public."ServerScan".http_server_hostname=public."HttpServer".hostname
        WHERE public."HttpServer".hostname='""" + hostname + """'
    """)
    res = cur.fetchall()
    cur.close()
    return jsonify(res)

def connect_db():
    """Connects to the PostgreSQL database."""
    conn = psycopg2.connect(host=app.config['POSTGRES_HOST'],
        database=app.config['POSTGRES_DB'], 
        user=app.config['POSTGRES_USER'], 
        password=app.config['POSTGRES_PASSWORD'])
    return conn

def get_db():
    """Opens a new database connection if there is none yet for the
    current application context.
    """
    if not hasattr(g, 'postgres_db'):
        g.postgres_db = connect_db()
    return g.postgres_db

@app.teardown_appcontext
def close_db(error):
    """Closes the database again at the end of the request."""
    if hasattr(g, 'postgres_db'):
        g.postgres_db.close()

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=80, debug=True)