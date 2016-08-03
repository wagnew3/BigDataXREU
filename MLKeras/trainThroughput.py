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
import math

import theano
import theano.tensor as T

maxTransferRate=4.8648958*10000000000

def readToLines(file):
    csvFile=open(file)
    lines=csvFile.read().splitlines()
    return lines

def readHistSizes(file):
    csvFile=open(file)
    lines=csvFile.read().splitlines()
    userHistSizes={}
    userID=''
    for line in lines:
        if line[0:5]=='user ':
            userID=line[5:len(line)]
            userHistSizes[userID]=[]
        else:
            userHistSizes[userID]+=[int(line)]
    return userHistSizes

def loadData(file):
    trainUserInOut={}
    tLines=readToLines(file)
    state=''
    userID=''
    lineNumber=0
    inputLen=0
    outputLen=0
    processed=0
    for tLine in tLines:
        lineNumber+=1
        if tLine=='in':
            state='in'
        elif tLine=='out':
            state='out'
        elif tLine[0:5]=='user ':
#             if processed>10:
#                 break;
#             processed+=1
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
                try:
                    floats+=[float(stringFloat)]
                except:
                    u=0
                if state=='out' and float(stringFloat)==0:
                    u=0
            trainUserInOut[userID][state]+=[floats]
            if len(floats)==4:
                u=0
            if state=='in':
                inputLen=len(floats)
            else:
                outputLen=len(floats)
    return [trainUserInOut, inputLen, outputLen]

def getTrainProgress(model, X, y, valX, validationOutputs):
    relativeTrainError=0.0
    attempedRelative=0
    absoluteTrainError=0.0
    preds=model.predict(X, batch_size=100, verbose=0)
    for valInd in range(0, len(X)):
        rate=y[valInd][0]*maxTransferRate
        predTP=preds[valInd][0]
        predRate=predTP*maxTransferRate
        if y[valInd]>0:
            relativeTrainError+=100*abs(preds[valInd][0]-y[valInd][0])/y[valInd][0]
            attempedRelative+=1
        else:
            u=0
        absoluteTrainError+=abs(predRate-rate)
    if attempedRelative>0:
        print('Train accuracy relative', relativeTrainError/attempedRelative)
    print('Train accuracy absolute', absoluteTrainError/len(X))
    
    relativeTrainError=0.0
    attempedRelative=0
    absoluteTrainError=0.0
    preds=model.predict(valX, batch_size=100, verbose=0)
    for valInd in range(0, len(valX)):
        rate=validationOutputs[valInd][0]*maxTransferRate
        predTP=preds[valInd][0]
        predRate=predTP*maxTransferRate
        if validationOutputs[valInd][0]>0:
            prevRelativeTrainError=relativeTrainError
            relativeTrainError+=100*abs(predRate-rate)/rate
            if math.isnan(relativeTrainError):
                print(predTP)
                u=validationOutputs[valInd][0]
                print(validationOutputs[valInd][0])
            attempedRelative+=1
        else:
            u=0
        absoluteTrainError+=abs(predRate-rate)
    if attempedRelative>0:
        print(attempedRelative)
        print(relativeTrainError)
        print('Validation accuracy relative', relativeTrainError/attempedRelative)
    print('Validation accuracy absolute', absoluteTrainError/len(valX))


result=loadData("/home/willie/workspace/MLKeras/data/throughputDataTrainSendInfo7.8")
trainUserInOut=result[0]
inputLen=result[1]
outputLen=result[2]        
print('inputLen', inputLen)
print('outputLen', outputLen)
 
validateUserInOut=loadData("/home/willie/workspace/MLKeras/data/throughputDataValidateSendInfo1.8")[0]

#userHistSizes=readHistSizes("/home/willie/workspace/MLKeras/heuristicsHistSizesvalidateAllFixedSrc")
           
# cut the text in semi-redundant sequences of maxlen characters
maxLen=20
step = 1

inputs=[]
outputs=[]
trainRecs=[]

validationInputs=[]
validationOutputs=[]
validationInputToUser={}
validationIndToHistSize=[]

# for userData in trainUserInOut:
#     for i in range(0, min(len(trainUserInOut[userData]['out']), len(trainUserInOut[userData]['in'])), step):
#         inputs+=[trainUserInOut[userData]['in'][i: min(len(trainUserInOut[userData]['in']), i+maxLen)]]
#         outputs+=[trainUserInOut[userData]['out'][min(len(trainUserInOut[userData]['out'])-1, i+maxLen-1)]]
#         trainRecs+=[userTrainRecs[userData][min(len(trainUserInOut[userData]['out'])-1, i+maxLen-1)]]
# print('nb training sequences:', len(inputs))

for userData in trainUserInOut:
    for i in range(1, min(len(trainUserInOut[userData]['out']), len(trainUserInOut[userData]['in']))+1, step):
        inputs+=[trainUserInOut[userData]['in'][max(0, i-maxLen): i]]
        outputs+=[trainUserInOut[userData]['out'][i-1]]
print('nb training sequences:', len(inputs))

for userData in validateUserInOut:
    for i in range(1, min(len(validateUserInOut[userData]['in']), len(validateUserInOut[userData]['out']))+1):
        tempValinputs=[]
        added=0
        if i<maxLen and trainUserInOut.has_key(userData) and len(trainUserInOut[userData]['in'])>0:
            tempValinputs+=trainUserInOut[userData]['in'][max(0, len(trainUserInOut[userData]['in'])-(maxLen-i)):len(trainUserInOut[userData]['in'])]
            added=1
        tempValinputs+=validateUserInOut[userData]['in'][max(0, i-maxLen): i]
        validationInputs+=[tempValinputs]
#         validationInputToUser[len(validationOutputs)]=userData
#         validationIndToHistSize+=[userHistSizes[userData][i-1]]
        validationOutputs+=[validateUserInOut[userData]['out'][i-1]]
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



inOuts=np.zeros((len(outputs), outputLen), dtype=np.float32)

# for i in range(0, len(validationInputs)):
#     inOut=[]
#     lastInput=validationInputs[i][len(validationInputs[i])-1]
#     for hInd in range(0, numHeurs):
#         for nInd in range(0, topN):
#             inOut+=[lastInput[hInd*topN*numHeurs+hInd*topN+nInd]]
#     inOuts[i]=inOut

# average=y.mean(0)
# for i in range(0, len(validationInputs)):
#     #inOuts[i]=average
#     inOuts[i][0]=1
#     inOuts[i][1]=0.75
#     inOuts[i][2]=0.5
# 
# print(inOuts[100]) 
# 
# X = np.zeros((len(inputs), inputLen), dtype=np.float32)
# y = np.zeros((len(outputs), outputLen), dtype=np.float32)

# for i in range(0, len(inputs)):
#     X[i] = inputs[i][0]
#     y[i] = outputs[i]

valX=np.zeros((len(validationInputs), maxLen, inputLen))
for valInd in range(0, len(validationInputs)):
    valX[valInd][0:len(validationInputs[valInd])]=validationInputs[valInd]

# valX=np.zeros((len(validationInputs), inputLen))
# for valInd in range(0, len(validationInputs)):
#     valX[valInd]=validationInputs[valInd][0]

epsilon = 1.0e-13
def relativeError(y_true, y_pred):
    diff = T.abs_((y_true - y_pred) / T.clip(T.abs_(y_true), epsilon, float('inf')))
    return 100. * T.mean(diff, axis=-1)

# build the model: 2 stacked LSTM
print('Build model...')
model = Sequential()
model.add(LSTM(15, return_sequences=True, input_shape=(maxLen, inputLen), consume_less="mem"))
model.add(LSTM(15, return_sequences=False, consume_less="mem"))
model.add(Dense(15))
model.add(Activation('relu'))
model.add(Dropout(0.2))
model.add(Dense(outputLen))
model.add(Activation('sigmoid'))
#model.load_weights('/home/willie/workspace/MLKeras/savedModels/ThroughputPredictionNoHeursRelError 90 0.h5')
model.compile(loss='mean_absolute_error', optimizer='rmsprop')
# getTrainProgress(model, X, y, trainRecs, valX, validationOutputs, validationRecs, validationInputToUser, validationIndToHistSize)
# for iteration in range(0, 20):
#     print()
#     print('-' * 50)
#     print('Iteration', iteration)
#     model.fit(X, inOuts, batch_size=5000, nb_epoch=1, verbose=0)
#     if(iteration%5==0):
#         getTrainProgress(model, X, y, trainRecs, valX, validationOutputs, validationRecs, validationInputToUser, validationIndToHistSize)

#train the model, output generated text after each iteration

#getTrainProgress(model, X, y, valX, validationOutputs)

# getTrainProgress(model, X, y, trainRecs, valX, validationOutputs, validationRecs, validationInputToUser, validationIndToHistSize)
for iteration in range(0, 1000):
    print()
    print('-' * 50)
    print('Iteration', iteration)
    history=model.fit(X, y, batch_size=5000, nb_epoch=1, verbose=0)
    print(history)
     
    if(iteration>0 and iteration%20==0):
        getTrainProgress(model, X, y, valX, validationOutputs)
        model.save_weights('/home/willie/workspace/MLKeras/savedModels/ThroughputPredictionNoHeursAbsError50kBatch10_15_7 '+str(iteration)+' 0.h5', overwrite=True)

# for valInd in range(0, len(validationInputs)):
#     if valInd%1000==0 and attempts>0:
#         print("val ind", valInd)
#         print('Validation accuracy', correct/attempts)
#     x=np.zeros((1, maxLen, inputLen))
#     x[0][0:len(validationInputs[valInd])]=validationInputs[valInd]
#     preds = model.predict(x, verbose=0)[0]
#     predAnswerInd=np.argmax(preds)
#     if predAnswerInd<outputLen:
#         cAnsAttempts+=1
#         maxInds=[]
#         maxVal=max(validationOutputs[valInd])
#         if maxVal>0:
#             for ind in range(0, len(validationOutputs[valInd])):
#                 if validationOutputs[valInd][ind]==maxVal:
#                     maxInds+=[ind]
#             if predAnswerInd in maxInds:
#                 correct+=1
#     attempts+=1

