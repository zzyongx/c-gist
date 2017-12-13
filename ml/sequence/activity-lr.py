from datetime import datetime
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from sklearn import metrics
from sklearn.model_selection import train_test_split
from sklearn.linear_model import LinearRegression

HOLIDAY2016_WHITELIST = set(["2016-01-01", "2016-01-08", "2016-01-09", "2016-01-10", "2016-01-11",
                             "2016-01-12", "2016-02-04", "2016-05-01", "2016-06-09", "2016-06-10",
                             "2016-09-15", "2016-09-16", "2016-10-03", "2016-10-04", "2016-10-05",
                             "2016-10-06", "2016-10-07"])
HOLIDAY2016_BLACKLIST = set(["2016-01-06", "2016-01-14", "2016-06-12", "2016-09-18", "2016-10-08", "2016-10-09"])

HOLIDAY2017_WHITELIST = set(["2017-01-02", "2017-01-27", "2017-01-30", "2017-01-31", "2017-02-01",
                             "2017-01-02", "2017-04-03", "2017-04-04", "2017-05-01", "2017-05-29",
                             "2017-05-30", "2017-10-02", "2017-10-03", "2017-10-04", "2017-10-05",
                             "2017-10-06", "2017-10-07"])
HOLIDAY2017_BLACKLIST = set(["2017-01-22", "2017-02-04", "2017-04-01", "2017-05-27", "2017-09-30"])

HOLIDAY_WHITELIST = HOLIDAY2016_WHITELIST | HOLIDAY2017_WHITELIST
HOLIDAY_BLACKLIST = HOLIDAY2016_BLACKLIST | HOLIDAY2017_BLACKLIST

def isholiday(date):
  if date in HOLIDAY_WHITELIST:
    return True
  elif date in HOLIDAY_BLACKLIST:
    return False
  else:
    day = datetime.strptime(date, "%Y-%m-%d").date().isoweekday()
    return day == 6 or day == 7

def prepare_data(data, period):
  data_x = []
  data_y = []

  for i in range(data.shape[0]-period+1):
    y = data[i+period-1]
    x = np.append(data[i:i+period-1].reshape(-1), y[1])
    data_x.append(x.tolist())
    data_y.append(y[0])

  return (data_x, data_y)

df = pd.read_csv('data/kpi.txt', sep = '\t', usecols =["kpi", "kpiDate"])
df["holiday"] = df[["kpiDate"]].applymap(lambda date: -0.01 if isholiday(date) else 0.01)
data = df.drop(columns = ["kpiDate"]).as_matrix()

data_x, data_y = prepare_data(data, 10)
(x_train, x_test, y_train, y_test) = train_test_split(data_x, data_y, test_size = 0.2, random_state = 10)

model = LinearRegression()
model.fit(x_train, y_train)

y_pred = model.predict(x_test)

print metrics.mean_squared_error(y_test, y_pred)
print metrics.r2_score(y_test, y_pred)

# use jupyter %run
plt.plot(y_test, "r-+", linewidth=2, label="test")
plt.plot(y_pred, "b-", linewidth=3, label="pred")
plt.show()
