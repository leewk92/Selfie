
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

import tarfile
import tensorflow.python.platform
from six.moves import urllib
import numpy as np
import tensorflow as tf
import nodelookup as nl
import os
import os.path
import gensim, logging

from tensorflow.python.platform import gfile


FLAGS = tf.app.flags.FLAGS

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



def create_graph():
  """"Creates a graph from saved GraphDef file and returns a saver."""
  # Creates graph from saved graph_def.pb.
  with gfile.FastGFile(os.path.join(
      FLAGS.model_dir, 'classify_image_graph_def.pb'), 'rb') as f:
    graph_def = tf.GraphDef()
    graph_def.ParseFromString(f.read())
    _ = tf.import_graph_def(graph_def, name='')


def run_category_inference_on_image(image):
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
    node_lookup = nl.NodeLookup()

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
