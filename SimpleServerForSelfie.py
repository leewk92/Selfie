
# Copyright 2015 Google Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ==============================================================================

"""Simple image classification with Inception.

Run image classification with Inception trained on ImageNet 2012 Challenge data
set.

This program creates a graph from a saved GraphDef protocol buffer,
and runs inference on an input JPEG image. It outputs human readable
strings of the top 5 predictions along with their probabilities.

Change the --image_file argument to any jpg image to compute a
classification of that image.

Please see the tutorial and website for a detailed description of how
to use this script to perform image recognition.

https://tensorflow.org/tutorials/image_recognition/
"""

import gensim, logging
import os

import os.path
import re
import sys
import tarfile

# pylint: disable=unused-import,g-bad-import-order
import tensorflow.python.platform
from six.moves import urllib
import numpy as np
import tensorflow as tf
# pylint: enable=unused-import,g-bad-import-order

from tensorflow.python.platform import gfile

from flask import Flask, request, json
from wsgiref.simple_server import make_server
import base64

app = Flask(__name__)

@app.route("/", methods=['POST'])
def api_upload():
    if request.headers['Content-Type'] == 'text/plain':
        #print (request.data)
        return 'OK', 200
    elif request.headers['Content-Type'] == 'application/json':
        jsonImage = eval(request.data)


        postimage = base64.decodestring(jsonImage['photo'])
        fh = open("tempPhoto.jpg", "wb")
        fh.write(postimage)
        fh.close()

        image = (FLAGS.image_file if FLAGS.image_file else
        os.path.join(FLAGS.model_dir, './tempPhoto.jpg'))
        category = run_inference_on_image(image)

        outputString = "{'category':'"+category+"'}"


        return json.loads(json.dumps(outputString)),200

        # print (json.dumps(request.json))
        # .insert(request.json)

        #return "OK", 200
    else:
        return "Unsupported Media Type", 415



class MySentences(object):
  def __init__(self, dirname):
    self.dirname = dirname

  def __iter__(self):
    for fname in os.listdir(self.dirname):
      print fname
      for line in open(os.path.join(self.dirname, fname)):
        yield line.split()


FLAGS = tf.app.flags.FLAGS

# classify_image_graph_def.pb:
#   Binary representation of the GraphDef protocol buffer.
# imagenet_synset_to_human_label_map.txt:
#   Map from synset ID to a human readable string.
# imagenet_2012_challenge_label_map_proto.pbtxt:
#   Text representation of a protocol buffer mapping a label to synset ID.
tf.app.flags.DEFINE_string(
    'model_dir', './',
    """Path to classify_image_graph_def.pb, """
    """imagenet_synset_to_human_label_map.txt, and """
    """imagenet_2012_challenge_label_map_proto.pbtxt.""")
tf.app.flags.DEFINE_string('image_file', '',
                           """Absolute path to image file.""")
tf.app.flags.DEFINE_integer('num_top_predictions', 5,
                            """Display this many predictions.""")

# pylint: disable=line-too-long
DATA_URL = 'http://download.tensorflow.org/models/image/imagenet/inception-2015-12-05.tgz'
# pylint: enable=line-too-long


class NodeLookup(object):
  """Converts integer node ID's to human readable labels."""

  def __init__(self,
               label_lookup_path=None,
               uid_lookup_path=None):
    if not label_lookup_path:
      label_lookup_path = os.path.join(
          FLAGS.model_dir, 'imagenet_2012_challenge_label_map_proto.pbtxt')
    if not uid_lookup_path:
      uid_lookup_path = os.path.join(
          FLAGS.model_dir, 'imagenet_synset_to_human_label_map.txt')
    self.node_lookup = self.load(label_lookup_path, uid_lookup_path)

  def load(self, label_lookup_path, uid_lookup_path):
    """Loads a human readable English name for each softmax node.

    Args:
      label_lookup_path: string UID to integer node ID.
      uid_lookup_path: string UID to human-readable string.

    Returns:
      dict from integer node ID to human-readable string.
    """
    if not gfile.Exists(uid_lookup_path):
      tf.logging.fatal('File does not exist %s', uid_lookup_path)
    if not gfile.Exists(label_lookup_path):
      tf.logging.fatal('File does not exist %s', label_lookup_path)

    # Loads mapping from string UID to human-readable string
    proto_as_ascii_lines = gfile.GFile(uid_lookup_path).readlines()
    uid_to_human = {}
    p = re.compile(r'[n\d]*[ \S,]*')
    for line in proto_as_ascii_lines:
      parsed_items = p.findall(line)
      uid = parsed_items[0]
      human_string = parsed_items[2]
      uid_to_human[uid] = human_string

    # Loads mapping from string UID to integer node ID.
    node_id_to_uid = {}
    proto_as_ascii = gfile.GFile(label_lookup_path).readlines()
    for line in proto_as_ascii:
      if line.startswith('  target_class:'):
        target_class = int(line.split(': ')[1])
      if line.startswith('  target_class_string:'):
        target_class_string = line.split(': ')[1]
        node_id_to_uid[target_class] = target_class_string[1:-2]

    # Loads the final mapping of integer node ID to human-readable string
    node_id_to_name = {}
    for key, val in node_id_to_uid.items():
      if val not in uid_to_human:
        tf.logging.fatal('Failed to locate: %s', val)
      name = uid_to_human[val]
      node_id_to_name[key] = name

    return node_id_to_name

  def id_to_string(self, node_id):
    if node_id not in self.node_lookup:
      return ''
    return self.node_lookup[node_id]


def create_graph():
  """"Creates a graph from saved GraphDef file and returns a saver."""
  # Creates graph from saved graph_def.pb.
  with gfile.FastGFile(os.path.join(
      FLAGS.model_dir, 'classify_image_graph_def.pb'), 'rb') as f:
    graph_def = tf.GraphDef()
    graph_def.ParseFromString(f.read())
    _ = tf.import_graph_def(graph_def, name='')


def run_inference_on_image(image):
  """Runs inference on an image.

  Args:
    image: Image file name.

  Returns:
    Nothing
  """
  if not gfile.Exists(image):
    tf.logging.fatal('File does not exist %s', image)
  image_data = gfile.FastGFile(image, 'rb').read()

  # Creates graph from saved GraphDef.
  create_graph()

  with tf.Session() as sess:
    # Some useful tensors:
    # 'softmax:0': A tensor containing the normalized prediction across
    #   1000 labels.
    # 'pool_3:0': A tensor containing the next-to-last layer containing 2048
    #   float description of the image.
    # 'DecodeJpeg/contents:0': A tensor containing a string providing JPEG
    #   encoding of the image.
    # Runs the softmax tensor by feeding the image_data as input to the graph.
    softmax_tensor = sess.graph.get_tensor_by_name('softmax:0')
    predictions = sess.run(softmax_tensor,
                           {'DecodeJpeg/contents:0': image_data})
    predictions = np.squeeze(predictions)

    # Creates node ID --> English string lookup.
    node_lookup = NodeLookup()

    top_k = predictions.argsort()[-FLAGS.num_top_predictions:][::-1]


    isMaximaCalculated = False
    model = gensim.models.Word2Vec.load('./model/totalmodel')
    for node_id in top_k:
      human_string = node_lookup.id_to_string(node_id)
      score = predictions[node_id]
      print('%s (score = %.5f)' % (human_string, score))
      #wonkyung add
      if isMaximaCalculated == False:
        category = calculate_similarity(model, human_string)
        isMaximaCalculated = True
  return category

def calculate_similarity(model, human_string):

  maxvalue_pet = 0
  maxword_pet = 'none'
  maxvalue_animal = 0
  maxword_animal = 'none'
  maxvalue_food = 0
  maxword_food = 'none'
  maxvalue_landscape = 0
  maxword_landscape = 'none'
  category = 'none'

  for word in human_string.split(' '):
    try:
      tmp_pet = model.similarity('pet',word)
      tmp_animal = model.similarity('animal',word)
      tmp_food = model.similarity('food',word)
      tmp_landscape = model.similarity('landscape',word)

      if tmp_pet > maxvalue_pet:
        maxvalue_pet = tmp_pet
        maxword_pet = word
        print 'pet', maxword_pet, maxvalue_pet
      if tmp_animal > maxvalue_animal:
        maxvalue_animal = tmp_animal
        maxword_animal = word
        print 'animal', maxword_animal, maxvalue_animal
      if tmp_food > maxvalue_food:
        maxvalue_food = tmp_food
        maxword_food = word
        print 'food', maxword_food, maxvalue_food
      if tmp_landscape > maxvalue_landscape:
        maxvalue_landscape = tmp_landscape
        maxword_landscape = word
        print 'landscape', maxword_landscape, maxvalue_landscape

    except:
      print('exception')

  maxvalue = max([ maxvalue_pet, maxvalue_food, maxvalue_landscape, maxvalue_animal])
  print 'maxValue', maxvalue, maxvalue_pet, maxvalue_animal, maxvalue_food, maxvalue_landscape
  if maxvalue == maxvalue_animal:
    maxword = maxword_animal
    category = 'Animals'
  elif maxvalue == maxvalue_pet:
    maxword = maxword_pet
    category = 'Animals'
  elif maxvalue == maxvalue_food:
    maxword = maxword_food
    category = 'Foods'
  elif maxvalue == maxvalue_landscape:
    maxword = maxword_landscape
    category = 'Landscapes'
    
  print category , ' : ', maxword, ' : ', maxvalue

  if maxvalue < 0.3:
    category = 'Etc'
    print 'Etc'

  return category

def maybe_download_and_extract():
  """Download and extract model tar file."""
  dest_directory = FLAGS.model_dir
  if not os.path.exists(dest_directory):
    os.makedirs(dest_directory)
  filename = DATA_URL.split('/')[-1]
  filepath = os.path.join(dest_directory, filename)
  if not os.path.exists(filepath):
    def _progress(count, block_size, total_size):
      sys.stdout.write('\r>> Downloading %s %.1f%%' % (
          filename, float(count * block_size) / float(total_size) * 100.0))
      sys.stdout.flush()
    filepath, _ = urllib.request.urlretrieve(DATA_URL, filepath,
                                             reporthook=_progress)
    print()
    statinfo = os.stat(filepath)
    print('Succesfully downloaded', filename, statinfo.st_size, 'bytes.')
  tarfile.open(filepath, 'r:gz').extractall(dest_directory)

def main(_):
  maybe_download_and_extract()
  httpd = make_server('', 8000, app)
  httpd.serve_forever()
  print 'HTTP Server Running...........'

if __name__ == '__main__':
  tf.app.run()
