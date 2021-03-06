from was import app, db
from was.models import *
from was.utils import token
from was.decorators import args, auth
from uuid import uuid4
from flask import request, jsonify, json
from pony.orm import db_session, core, select
from datetime import datetime, timedelta
from daemon import client

def _make_session(sessionOwner, sessionType):
    old_session = Session.get(lambda s:s.sessionOwner == sessionOwner)
    if old_session:
        old_session.delete()

    return Session(session = token.model_unique_id(Session),
                   refreshToken = str(uuid4()), sessionType = sessionType, 
                   sessionOwner = sessionOwner)

def _hide_privates(session):
    session['expires'] = session['expires'].strftime('%s')
    for key in ['sessionType', 'sessionOwner', 'enabled', 'createDate']:
        del(session[key])
    return session

def add_relation_node_to_user(nodeId, nickName, user, is_virtual=False, is_owner=True):
    if not Node.get(nodeId = nodeId, virtual=is_virtual):
        Node(nodeId = nodeId, virtual=is_virtual)
    RegisteredNode(owner = is_owner, nickName = nickName, user = user, node = nodeId)
    client.publish('/user/%s/register' % user, json.dumps({'node': nodeId, 'nickName': nickName, 'owner': is_owner}))

@app.route('/session/createUser', methods=['POST'])
@auth.is_valid_client()
@args.is_exists(body=['email', 'password'])
@db_session
def createUser():
    try:
        payload = request.get_json()
        if User.get(email=payload['email'], password=payload['password']):
            session = _make_session(payload['email'], 'user')
            db.commit()
            return jsonify({'statusCode': 200, 'result': _hide_privates(session.to_dict())})
        else:
            raise ValueError
    except ValueError:
        return jsonify({'statusCode': 400, 'result': 'Invalid user information'})

@app.route('/session/createNode', methods=['POST'])
@auth.is_valid_client()
@args.is_exists(body=['nodeId'])
@db_session
def createNode():
    nodeId = request.get_json()['nodeId']
    registeredNode = Node.get(nodeId=nodeId)
    pendingNode = PendingRequestNode.get(nodeId = nodeId)
    if not registeredNode and (not pendingNode or pendingNode.endDate < datetime.now()):
        return jsonify({'statusCode': 400, 'result': 'Unregisterd nodeId'})
    else:
        if pendingNode:
            add_relation_node_to_user(pendingNode.nodeId, pendingNode.nickName, pendingNode.user.email)
            session = _make_session(pendingNode.nodeId, 'node')
            pendingNode.delete()
            db.commit()
        else:
            session = Session.get(sessionType='node', sessionOwner=nodeId, enabled=True)
        return jsonify({'statusCode': 200, 'result': _hide_privates(session.to_dict())})

@app.route('/session/refresh', methods=['POST'])
@auth.is_valid_client()
@args.is_exists(body=['session', 'refreshToken'])
@db_session
def refresh():
    payload = request.get_json()
    session = Session.get(session=payload['session'], refreshToken=payload['refreshToken'])
    if not session or not session.enabled:
        return jsonify({'statusCode': 400, 'result': 'Invalid session or refreshToken'})
    else:
        session.expires = datetime.now() + timedelta(hours=2)
        db.commit()
        return jsonify({'statusCode': 200, 'result': _hide_privates(session.to_dict())})
