import keras, cv2
from keras.datasets import mnist
from keras.models import Sequential
from keras.layers import Dense, Dropout, Flatten
from keras.layers import Conv2D, MaxPooling2D
from keras.optimizers import SGD
from keras import backend as K

(x_trainingData, y_trainingData), (x_testingData, y_testingData) = mnist.load_data()
# print(x_trainingData.shape, y_trainingData.shape)
height = x_trainingData[0].shape[0]//2
width = x_trainingData[0].shape[1]//2
x_trainingData = x_trainingData[:, height:, width:]
x_testingData = x_testingData[:, height:, width:]
# print(x_trainingData.shape)

num_classes = 10
x_trainingData = x_trainingData.reshape(x_trainingData.shape[0], height, width, 1)
# print(x_trainingData.shape)
x_testingData = x_testingData.reshape(x_testingData.shape[0], height, width, 1)
y_trainingData = keras.utils.to_categorical(y_trainingData, num_classes)
y_testingData = keras.utils.to_categorical(y_testingData, num_classes)

x_trainingData = x_trainingData.astype('float32')
x_testingData = x_testingData.astype('float32')
x_trainingData /= 255
x_testingData /= 255
# print('x_trainingData shape:', x_trainingData.shape)
# print(x_trainingData.shape[0], 'train samples')
# print(x_testingData.shape[0], 'test samples')


# Compiling the machine learning model
batch_size = 32
epochs = 10
input_shape = (14, 14, 1)
model = Sequential()
model.add(Conv2D(32, kernel_size=(3, 3),activation='relu',kernel_initializer='he_uniform',input_shape=input_shape))
model.add(MaxPooling2D(pool_size=(2, 2)))
model.add(Conv2D(64, (3, 3), activation='relu',kernel_initializer='he_uniform'))
model.add(Conv2D(64, (3, 3), activation='relu',kernel_initializer='he_uniform'))
model.add(MaxPooling2D(pool_size=(2, 2)))
model.add(Flatten())
model.add(Dense(100, activation='relu',kernel_initializer='he_uniform'))
model.add(Dense(num_classes, activation='softmax'))
currentOptimizer = SGD(learning_rate=0.01,momentum=0.9)
model.compile(loss=keras.losses.categorical_crossentropy,optimizer=currentOptimizer,metrics=['accuracy'])

# Training the model
hist = model.fit(x_trainingData, y_trainingData,batch_size=batch_size,epochs=epochs,verbose=1,validation_data=(x_testingData, y_testingData))
print("The model has successfully trained")
model.save('deepLearningModelPart4')
print("Saving the model as deepLearningModel")

score = model.evaluate(x_testingData, y_testingData, verbose=0)