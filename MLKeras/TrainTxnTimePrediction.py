'''Example script to generate text from Nietzsche's writings.
At least 20 epochs are required before the generated text
starts sounding coherent.
It is recommended to run this script on GPU, as recurrent
networks are quite computationally intensive.
If you try this script on new data, make sure your corpus
has at least ~100k characters. ~1M is better.
'''

from __future__ import print_function
from keras.models import Sequential
from keras.layers import Dense, Activation, Dropout
from keras.layers import LSTM
from keras.utils.data_utils import get_file
import numpy as np
import random
import sys

def readToLines(file):
    csvFile=open(file)
    lines=csvFile.read().splitlines()
    return lines

trainUserInOut={}
processed=0
tLines=readToLines("/home/willie/workspace/Globus/data/time_pred/heuristicsData_time_train")
state=''
userID=''
inputLen=1
outputLen=1
lineNumber=0
for tLine in tLines:
    lineNumber+=1
    if tLine=='in':
        state='in'
    elif tLine=='out':
        state='out'
    elif tLine[0:5]=='user ':
#         if processed>10:
#             break;
#         processed+=1
        userID=tLine[5:len(tLine)]
        trainUserInOut[userID]={}
        trainUserInOut[userID]['in']=[]
        trainUserInOut[userID]['out']=[]
        
        if len(trainUserInOut)%1000==0:
            print("len user id", len(trainUserInOut))
    else:
        stringFloats=tLine.split(',')
        floats=[]
        for stringFloat in stringFloats:
            floats+=[float(stringFloat)]
        trainUserInOut[userID][state]+=[floats]
            
print('inputLen', inputLen)
print('outputLen', outputLen)
        
validateUserInOut={}
tLines=readToLines("/home/willie/workspace/Globus/data/time_pred/heuristicsData_time_validate")
state=''
userID=''
processed=0
for tLine in tLines:
    if tLine=='in':
        state='in'
    elif tLine=='out':
        state='out'
    elif tLine[0:5]=='user ':
#         if processed>10:
#             break;
#         processed+=1
        userID=tLine[5:len(tLine)]
        validateUserInOut[userID]={}
        validateUserInOut[userID]['in']=[]
        validateUserInOut[userID]['out']=[]
        if len(validateUserInOut)%1000==0:
            print("len user id", len(validateUserInOut))
    else:
        stringFloats=tLine.split(',')
        floats=[]
        for stringFloat in stringFloats:
            floats+=[float(stringFloat)]
        validateUserInOut[userID][state]+=[floats]
            
# cut the text in semi-redundant sequences of maxlen characters
maxLen=50
step = 1

inputs=[]
outputs=[]

validationInputs=[]
validationOutputs=[]

for userData in trainUserInOut:
    for i in range(0, min(len(trainUserInOut[userData]['out']), len(trainUserInOut[userData]['in'])), step):
        inputs+=[trainUserInOut[userData]['in'][i: min(len(trainUserInOut[userData]['in']), i+maxLen)]]
        outputs+=[trainUserInOut[userData]['out'][min(len(trainUserInOut[userData]['out'])-1, i+maxLen-1)]]
        if i%10000==0:
            print("train inputs", i)
print('nb training sequences:', len(inputs))

for userData in validateUserInOut:
    for i in range(1, min(len(validateUserInOut[userData]['in']), len(validateUserInOut[userData]['out']))):
        if i<maxLen and trainUserInOut.has_key(userData) and len(trainUserInOut[userData]['in'])>0:
            validationInputs+=[trainUserInOut[userData]['in'][max(0, len(trainUserInOut[userData]['in'])-(maxLen-i)):len(trainUserInOut[userData]['in'])]]
        validationInputs+=[validateUserInOut[userData]['in'][max(0, i-maxLen): i]]
        validationOutputs+=[validateUserInOut[userData]['out'][i]]
print('nb validation sequences:', len(validationInputs))

print('Vectorization...')
X = np.zeros((len(inputs), maxLen, inputLen), dtype=np.float32)
y = np.zeros((len(outputs), outputLen), dtype=np.float32)

for i in range(0, len(inputs)):
    for j in range(0, len(inputs[i])):
        try:
            X[i, j] = inputs[i][j]
        except Exception as e:
            print(inputs[i][j])
    y[i] = outputs[i]


# build the model: 2 stacked LSTM
print('Build model...')
model = Sequential()
model.add(LSTM(256, return_sequences=True, input_shape=(maxLen, inputLen)))
model.add(LSTM(256, return_sequences=False))
model.add(Dropout(0.2))
model.add(Dense(outputLen))
model.add(Activation('sigmoid'))

model.compile(loss='binary_crossentropy', optimizer='rmsprop')


def sample(a, temperature=1.0):
    # helper function to sample an index from a probability array
    a = np.log(a) / temperature
    a = np.exp(a) / np.sum(np.exp(a))
    return np.argmax(np.random.multinomial(1, a, 1))

# train the model, output generated text after each iteration
for iteration in range(1, 2):
    print()
    print('-' * 50)
    print('Iteration', iteration)
    model.fit(X, y, batch_size=128, nb_epoch=1)
    model.save_weights('/home/willie/workspace/Globus/data/time_pred/SavedNetworks/TimePred'+str(iteration)+' 1.h5')

totalMDist=0.0

for valInd in range(0, len(validationInputs)):
    if valInd%1000==0 and valInd>0:
        print("val ind", valInd)
        print('Time Validation accuracy', totalMDist/valInd)
    #valInd=random.randint(0, len(validationInputs)-1)
    x=np.zeros((1, maxLen, inputLen))
    x[0][0:len(validationInputs[valInd])]=validationInputs[valInd]
    preds = model.predict(x, verbose=0)[0]
    totalMDist+=abs(preds[0]-validationOutputs[valInd][0])
print('Final Time Validation accuracy', totalMDist)