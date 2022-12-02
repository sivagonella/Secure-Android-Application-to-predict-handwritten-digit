from keras.models import load_model
import cv2, os
import numpy as np

filename = "Slide3.png"
image = cv2.imread(filename, cv2.IMREAD_GRAYSCALE)
image = cv2.resize(image, (28,28))
image = np.array(image)
image = image.reshape(1,28,28,1)
# image = 255 - image
image = image/255.0

model = load_model('deepLearningModel')

res = model.predict([image])[0]
number = np.argmax(res)
print(str(number))
path = str(number) + '/'

if(os.path.exists(path)):
    os.rename(filename, path+filename)
    
else:
    os.mkdir(path)
    os.rename(filename, path + filename)