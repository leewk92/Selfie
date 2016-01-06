

import tensorflow as tf
from flask import Flask, request, json
from wsgiref.simple_server import make_server
import base64
import imagecategorizer as ic
import os
import os.path

app = Flask(__name__)

@app.route("/", methods=['POST'])
def api_upload():
    if request.headers['Content-Type'] == 'text/plain':
        return 'OK', 200
    elif request.headers['Content-Type'] == 'application/json':
        jsonData = json.loads(request.data)


        postImage = base64.decodestring(jsonData['photo'])
        imageFile = open("tempPhoto.jpg", "wb")
        imageFile.write(postImage)
        imageFile.close()

        imagePath = './tempPhoto.jpg'
        category = ic.run_category_inference_on_image(imagePath)
        outputJson = dict()
        outputJson['category'] = category
        outputString = json.dumps(outputJson)

        return outputString,200

    else:
        return "Unsupported Media Type", 415


def main(_):
  ic.maybe_download_and_extract()
  httpd = make_server('', 8000, app)
  httpd.serve_forever()
  print 'HTTP Server Running...........'

if __name__ == '__main__':
  tf.app.run()
