import numpy as np
from sklearn import metrics
from sklearn.linear_model import LinearRegression

def genx(size):
  return 3 * np.random.rand(size, 2)

def linear(X, noise = 1):
  return 2 + np.matmul(X, np.array([[1],[4]])) + np.random.randn(X.shape[0], 1) * noise

def perform(X_test, Y_test, Y_pred):
  print "X test:", X_test
  print "Y test:", Y_test
  print "Y pred:", Y_pred
  print "mean_squared_error:", metrics.mean_squared_error(Y_test, Y_pred)
  print "r2_score:", metrics.r2_score(Y_test, Y_pred)

noises = [0.1, 0.5, 1, 1.5]

print "LinearRegression"
for noise in noises:
  X = genx(100)
  Y = linear(X, noise)[:, 0]

  model = LinearRegression()
  model.fit(X, Y)

  X_test = genx(5)
  perform(X_test, linear(X_test, 0)[:, 0], model.predict(X_test))

print "Normal Equation"
for noise in noises:
  X = genx(100)
  Y = linear(X, noise)
  X_b = np.c_[np.ones((100, 1)), X]

  theta_best = np.linalg.inv(X_b.T.dot(X_b)).dot(X_b.T).dot(Y)

  X_test = genx(5)
  Y_pred = np.array(np.c_[np.ones((X_test.shape[0], 1)), X_test]).dot(theta_best)
  perform(X_test, linear(X_test, 0)[:, 0], Y_pred[:, 0])
