
# coding: utf-8



import numpy
import matplotlib.pyplot as plt
from pandas import read_csv
import math
from keras.models import Sequential
from keras.layers import Dense
from keras.layers import LSTM
from sklearn.preprocessing import MinMaxScaler
from sklearn.metrics import mean_squared_error
get_ipython().run_line_magic('matplotlib', 'inline')




# load the dataset
dataframe = read_csv('/Users/dawnchau/accessRecord.csv', usecols=[1], engine='python', skipfooter=0)
dataset = dataframe.values
dataset = dataset.reshape(len(dataset),1)
print(dataset.shape)
# dataset = dataset[0:len(dataset)-1]
# 将整型变为float
print(dataset.shape)
dataset = dataset.astype('float32')
plt.plot(dataset)
plt.show()
print(len(dataset))


# In[294]:


# X is the number of passengers at a given time (t) and Y is the number of passengers at the next time (t + 1).

# convert an array of values into a dataset matrix
def create_dataset(dataset, look_back=1):
  dataX, dataY = [], []
  for i in range(len(dataset)-look_back):
    a = dataset[i:(i+look_back), 0]
    dataX.append(a)
    dataY.append(dataset[i + look_back, 0])
  return numpy.array(dataX), numpy.array(dataY)

# fix random seed for reproducibility
numpy.random.seed(7)


# In[295]:


# normalize the dataset
scaler = MinMaxScaler(feature_range=(0, 1))
dataset = scaler.fit_transform(dataset)


# split into train and test sets
train_size = 179
test_size = len(dataset) - train_size
train, test = dataset[0:train_size,:], dataset[train_size:len(dataset),:]
print(len(train))
print(len(test))


# In[296]:


look_back = 1
trainX, trainY = create_dataset(train, look_back)

testX = []
testX.append(train[len(train)-1:,0])
testX = numpy.array(testX)

testY = dataset[len(dataset)-1]
print(trainY.shape)
print(train)
print(test)
print(trainX)
print(trainY)
print(testX)
print(testY)


# In[297]:


trainX = numpy.reshape(trainX, (trainX.shape[0], 1, trainX.shape[1]))
testX = numpy.reshape(testX, (testX.shape[0], 1, testX.shape[1]))


# In[298]:


model = Sequential()
model.add(LSTM(4, input_shape=(1, look_back)))
model.add(Dense(1))
model.compile(loss='mean_squared_error', optimizer='adam')
model.fit(trainX, trainY, epochs=100, batch_size=1, verbose=2)


# In[299]:


trainPredict = model.predict(trainX)
print(trainX.shape)
print(testX.shape)
testPredict = model.predict(testX)


# In[300]:


trainPredict = scaler.inverse_transform(trainPredict)
trainY = scaler.inverse_transform([trainY])
testPredict = scaler.inverse_transform(testPredict)
testY = scaler.inverse_transform([testY])


# In[301]:


trainScore = math.sqrt(mean_squared_error(trainY[0], trainPredict[:,0]))
print('Train Score: %.2f RMSE' % (trainScore))
testScore = math.sqrt(mean_squared_error(testY[0], testPredict[:,0]))
print('Test Score: %.2f RMSE' % (testScore))


# In[302]:


trainPredictPlot = numpy.empty_like(dataset)
trainPredictPlot[:, :] = numpy.nan
trainPredictPlot[look_back:len(trainPredict)+look_back, :] = trainPredict

# shift test predictions for plotting
testPredictPlot = numpy.empty_like(dataset)
testPredictPlot[:, :] = numpy.nan
testPredictPlot[len(dataset)-1, :] = testPredict
print(trainPredictPlot)
print(testPredictPlot)

# plot baseline and predictions
plt.plot(scaler.inverse_transform(dataset))
plt.plot(trainPredictPlot)
plt.scatter(len(dataset)-1,testPredictPlot[len(dataset)-1])
plt.show()


# In[312]:


testPredictPlot = numpy.empty_like(dataset)
testPredictPlot[:, :] = numpy.nan

for i in range(125,180):
  train_size = i
  test_size = 1
  train, test = dataset[0:train_size,:], dataset[train_size:train_size+1,:]
  look_back = 3
  trainX, trainY = create_dataset(train, look_back)

  testX = []
  testX.append(train[len(train)-look_back:,0])
  testX = numpy.array(testX)

  testY = test[0]
  trainX = numpy.reshape(trainX, (trainX.shape[0], 1, trainX.shape[1]))
  testX = numpy.reshape(testX, (testX.shape[0], 1, testX.shape[1]))
  model = Sequential()
  model.add(LSTM(4, input_shape=(1, look_back)))
  model.add(Dense(1))
  model.compile(loss='mean_squared_error', optimizer='adam')
  model.fit(trainX, trainY, epochs=10, batch_size=1, verbose=2)
  testPredict = model.predict(testX)
  testPredict = scaler.inverse_transform(testPredict)
  testY = scaler.inverse_transform([testY])
  testPredictPlot[i, :] = testPredict

  plt.xlabel('time')
  plt.ylabel('access acout')
  plt.plot(scaler.inverse_transform(dataset))
  plt.plot(testPredictPlot)
  plt.show()
    

