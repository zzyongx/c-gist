import os
import tensorflow as tf
from tensorflow.contrib import rnn
from tensorflow.examples.tutorials.mnist import input_data

os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'

# http://t.cn/R08t6hT

# mnist = input_data.read_data_sets("./data/mnist/", one_hot = True, source_url = "http://yann.lecun.com/exdb/mnist/")

# modify /usr/lib/python2.7/site-packages/tensorflow/contrib/learn/python/learn/datasets/mnist.py SOURCE_URL
mnist = input_data.read_data_sets("./data/mnist/", one_hot = True)

# image size 28x28, unrolled through 28 time steps
time_steps = 28
# hidden LSTM units
num_units = 128
# rows of 28 pixels
n_input = 28
# 0-9 10
nclass = 10


W = tf.Variable(tf.random_normal([num_units, nclass]), name = 'weight')
b = tf.Variable(tf.random_normal([nclass]), name = 'bias')

X = tf.placeholder(tf.float32, [None, time_steps, n_input])
Y = tf.placeholder(tf.float32, [None, nclass])

# shape(?, 28)
input = tf.unstack(X, time_steps, 1)

# define the network
lstm_layer = rnn.BasicLSTMCell(num_units, forget_bias = 1)
outputs, _ = rnn.static_rnn(lstm_layer, input, dtype = tf.float32)

prediction = tf.matmul(outputs[-1], W) + b

cost = tf.reduce_mean(tf.nn.softmax_cross_entropy_with_logits(logits = prediction, labels = Y))
train = tf.train.AdamOptimizer(learning_rate = 0.001).minimize(cost)

correct_prediction = tf.equal(tf.argmax(prediction, 1), tf.argmax(Y, 1))
accuracy = tf.reduce_mean(tf.cast(correct_prediction, tf.float32))

with tf.Session() as session:
  batch_size = 128
  session.run(tf.global_variables_initializer())
  for step in range(801):
    batch_x, batch_y = mnist.train.next_batch(batch_size = batch_size)
    batch_x = batch_x.reshape(batch_size, time_steps, n_input)
    session.run(train, feed_dict = {X: batch_x, Y: batch_y})

    if step % 100 == 0:
      step_accuracy = session.run(accuracy, feed_dict = {X: batch_x, Y: batch_y})
      step_cost     = session.run(cost, feed_dict = {X: batch_x, Y: batch_y})
      print("[{}] Accuracy: {} Cost: {}".format(step, step_accuracy, step_cost))

  xtest = mnist.test.images[:128].reshape((-1, time_steps, n_input))
  label = mnist.test.labels[:128]
  print("Testing Accuracy:", session.run(accuracy, feed_dict = {X: xtest, Y:label}))
