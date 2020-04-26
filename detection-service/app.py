from flask import Flask
app = Flask(__name__)

# Asynchronous Request-Response pattern
# CorrelationID is provided as record key
# CorrelationID is the Base64 encoding of the record value
# The produced record value is the domain scan list
# The consumed record value is the domains using Nginx
@app.route('/scan', methods=['POST'])
def scan():
    return None

@app.route('/scan/<domain>')
def getDomainScan():
    return None