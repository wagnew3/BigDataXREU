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
tLines=readToLines("/home/willie/workspace/MLKeras/heuristicsDatatrainAllFixedDst")
state=''
userID=''
inputLen=0
outputLen=0;
lineNumber=0
for tLine in tLines:
    lineNumber+=1
    if tLine=='in':
        state='in'
    elif tLine=='out':
        state='out'
    elif tLine[0:5]=='user ':
#         if processed>100:
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
        if len(floats)==4:
            u=0
        if state=='in':
            inputLen=len(floats)
        else:
            outputLen=len(floats)
            
print('inputLen', inputLen)
print('outputLen', outputLen)

userTrainRecs
        
validateUserInOut={}
tLines=readToLines("/home/willie/workspace/MLKeras/heuristicsDatavalidateAllFixedDst")
state=''
userID=''
processed=0
for tLine in tLines:
    if tLine=='in':
        state='in'
    elif tLine=='out':
        state='out'
    elif tLine[0:5]=='user ':
#         if processed>100:
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
maxLen=20
step = 1

inputs=[]
outputs=[]

validationInputs=[]
validationOutputs=[]

for userData in trainUserInOut:
    for i in range(0, min(len(trainUserInOut[userData]['out']), len(trainUserInOut[userData]['in'])), step):
        inputs+=[trainUserInOut[userData]['in'][i: min(len(trainUserInOut[userData]['in']), i+maxLen)]]
        outputs+=[trainUserInOut[userData]['out'][min(len(trainUserInOut[userData]['out'])-1, i+maxLen-1)]]
print('nb training sequences:', len(inputs))

for userData in validateUserInOut:
    for i in range(1, min(len(validateUserInOut[userData]['in']), len(validateUserInOut[userData]['out']))):
        tempValinputs=[]
        if i<maxLen and trainUserInOut.has_key(userData) and len(trainUserInOut[userData]['in'])>0:
            tempValinputs+=trainUserInOut[userData]['in'][max(0, len(trainUserInOut[userData]['in'])-(maxLen-i)):len(trainUserInOut[userData]['in'])]
        tempValinputs+=validateUserInOut[userData]['in'][max(0, i-maxLen): i]
        validationInputs+=[tempValinputs]
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

    
average=y.mean(0)
print(average)
averageOuts=np.zeros((len(outputs), outputLen), dtype=np.float32)
 
for i in range(0, len(inputs)):
    averageOuts[i]=average
    
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

# build the model: 2 stacked LSTM
print('Build model...')
model = Sequential()
model.add(LSTM(10, return_sequences=True, input_shape=(maxLen, inputLen), consume_less="mem"))
model.add(LSTM(10, return_sequences=False, consume_less="mem"))
model.add(Dense(10))
model.add(Activation('relu'))
model.add(Dropout(0.2))
model.add(Dense(outputLen))
model.add(Activation('softmax'))


#model.load_weights('/home/willie/workspace/MLKeras/savedModels/AllFixedTop315Small12kBPred0 1.h5')



model.compile(loss='categorical_crossentropy', optimizer='rmsprop')

for iteration in range(0, 20):
    print()
    print('-' * 50)
    print('Iteration', iteration)
    model.fit(X, averageOuts, batch_size=5000, nb_epoch=1, verbose=0)
    
    if(iteration%5==0):
        attempts=0.0
        correct=0.0       
        preds = model.predict(X, batch_size=25000, verbose=0)
        for valInd in range(0, len(X)):
            predAnswerInd=np.argmax(preds[valInd])
            maxInds=[]
            maxVal=max(y[valInd])
            if maxVal>0:
                for ind in range(0, len(y[valInd])):
                    if y[valInd][ind]==maxVal:
                        maxInds+=[ind]
                if predAnswerInd in maxInds:
                    correct+=1
            attempts+=1
        print('Train accuracy', correct/attempts)
        
        attempts=0.0
        correct=0.0 
        topOneCorrect=0.0;
        preds = model.predict(valX, batch_size=1000, verbose=0)
        for valInd in range(0, len(validationInputs)):
            maxInds=[]
            maxVal=max(validationOutputs[valInd])
            if maxVal>0:
                for ind in range(0, len(validationOutputs[valInd])):
                    if validationOutputs[valInd][ind]==maxVal:
                        maxInds+=[ind]
                
                predAnswerInd=np.argmax(preds[valInd])
                if predAnswerInd in maxInds:
                    topOneCorrect+=1
                        
                for nInd in range(0, 3):
                    predAnswerInd=np.argmax(preds[valInd])
                    preds[valInd][predAnswerInd]=float("-inf")
                    if predAnswerInd in maxInds:
                        correct+=1
                        break
            attempts+=1
        print('Validation accuracy', correct/attempts)
        print('Validation top one', topOneCorrect/attempts)

#train the model, output generated text after each iteration
for iteration in range(0, 1000):
    print()
    print('-' * 50)
    print('Iteration', iteration)
    model.fit(X, y, batch_size=5000, nb_epoch=1, verbose=0)
    
    if(iteration%1==0):
        attempts=0.0
        correct=0.0       
        preds = model.predict(X, batch_size=25000, verbose=0)
        for valInd in range(0, len(X)):
            predAnswerInd=np.argmax(preds[valInd])
            maxInds=[]
            maxVal=max(y[valInd])
            if maxVal>0:
                for ind in range(0, len(y[valInd])):
                    if y[valInd][ind]==maxVal:
                        maxInds+=[ind]
                if predAnswerInd in maxInds:
                    correct+=1
            attempts+=1
        print('Train accuracy', correct/attempts)
        
        attempts=0.0
        correct=0.0 
        topOneCorrect=0.0;
        preds = model.predict(valX, batch_size=1000, verbose=0)
        for valInd in range(0, len(validationInputs)):
            maxInds=[]
            maxVal=max(validationOutputs[valInd])
            if maxVal>0:
                for ind in range(0, len(validationOutputs[valInd])):
                    if validationOutputs[valInd][ind]==maxVal:
                        maxInds+=[ind]
                
                predAnswerInd=np.argmax(preds[valInd])
                if predAnswerInd in maxInds:
                    topOneCorrect+=1
                        
                for nInd in range(0, 3):
                    predAnswerInd=np.argmax(preds[valInd])
                    preds[valInd][predAnswerInd]=float("-inf")
                    if predAnswerInd in maxInds:
                        correct+=1
                        break
            attempts+=1
        print('Validation accuracy', correct/attempts)
        print('Validation top one', topOneCorrect/attempts)
    
    model.save_weights('/home/willie/workspace/MLKeras/savedModels/AllFixedTop310Small12kBrcDstPred'+str(iteration)+' 7.h5')

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


