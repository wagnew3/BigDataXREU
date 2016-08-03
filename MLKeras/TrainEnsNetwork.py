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
                floats+=[float(stringFloat)]
            trainUserInOut[userID][state]+=[floats]
            if len(floats)==4:
                u=0
            if state=='in':
                inputLen=len(floats)
            else:
                outputLen=len(floats)
    return [trainUserInOut, inputLen, outputLen]

def loadRecs(file):
    userRecs={}
    tLines=readToLines(file)
    userID=''
    for tLine in tLines:
        if tLine[0:5]=='user ':
            userID=tLine[5:len(tLine)]
            userRecs[userID]=[]
        else:
            stringRecs=tLine.split(',')
            userRecs[userID]+=[stringRecs]
    return userRecs

def getTrainProgress(model, X, y, trainRecs, valX, validationOutputs, validationRecs, validationInputToUser, validationIndToHistSize):
    attempts=0.0
    correct=0.0  
    maxGuesses=3  
       
    preds=model.predict(X, batch_size=100, verbose=0)
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
    preds = model.predict(X, batch_size=100, verbose=0)
    for valInd in range(0, len(X)):
        maxInds=[]
        maxVal=max(y[valInd])
        if maxVal>0:
            for ind in range(0, len(y[valInd])):
                if y[valInd][ind]==maxVal:
                    maxInds+=[ind]       
            predAnswerInd=np.argmax(preds[valInd])
            if predAnswerInd in maxInds:
                topOneCorrect+=1
            guessed=[]
            numberGuesses=0        
            while numberGuesses<maxGuesses:
                predAnswerInd=np.argmax(preds[valInd])
                preds[valInd][predAnswerInd]=float("-inf")
                #if not trainRecs[valInd][predAnswerInd] in guessed:
                #guessed+=trainRecs[valInd][predAnswerInd]
                numberGuesses+=1
                if predAnswerInd in maxInds:
                    correct+=1
                    break
        attempts+=1
    print('Train accuracy', correct/attempts)

    valUsersAccuracies={}
    valUsersAccuraciesTopOne={}
    valHSAccuracies={}
    valHSAAccuraciesTopOne={}
    attempts=0.0
    correct=0.0 
    topOneCorrect=0.0;
    nonHistGuesses=0
    preds = model.predict(valX, batch_size=100, verbose=0)
    for valInd in range(0, len(valX)):
        histSize=validationIndToHistSize[valInd]
        if not valHSAccuracies.has_key(histSize):
            valHSAccuracies[histSize]=[0.0,0.0]
            valHSAAccuraciesTopOne[histSize]=[0.0,0.0]
        if not valUsersAccuracies.has_key(validationInputToUser[valInd]):
            valUsersAccuracies[validationInputToUser[valInd]]=[0.0,0.0]
            valUsersAccuraciesTopOne[validationInputToUser[valInd]]=[0.0,0.0]
        maxInds=[]
        maxVal=max(validationOutputs[valInd])
        if maxVal>0:
            for ind in range(0, len(validationOutputs[valInd])):
                if validationOutputs[valInd][ind]==maxVal:
                    maxInds+=[ind]       
            predAnswerInd=np.argmax(preds[valInd])
            if predAnswerInd in maxInds:
                topOneCorrect+=1
                valUsersAccuraciesTopOne[validationInputToUser[valInd]][0]+=1
                valHSAAccuraciesTopOne[histSize][0]+=1
            numberGuesses=0        
            while numberGuesses<maxGuesses:
                predAnswerInd=np.argmax(preds[valInd])
                if not predAnswerInd<3:
                    nonHistGuesses+=1
                preds[valInd][predAnswerInd]=float("-inf")
                numberGuesses+=1
                if predAnswerInd in maxInds:
                    correct+=1
                    valUsersAccuracies[validationInputToUser[valInd]][0]+=1
                    valHSAccuracies[histSize][0]+=1
                    break
        attempts+=1
        valUsersAccuraciesTopOne[validationInputToUser[valInd]][1]+=1
        valUsersAccuracies[validationInputToUser[valInd]][1]+=1
        valHSAAccuraciesTopOne[histSize][1]+=1
        valHSAccuracies[histSize][1]+=1
        
    totalUserAccTopOne=0
    for accData in valUsersAccuraciesTopOne.values():
        totalUserAccTopOne+=accData[0]/accData[1]
    totalUserAcc=0
    for accData in valUsersAccuracies.values():
        totalUserAcc+=accData[0]/accData[1]
         
    valMatheString='{'
    for accData in valHSAccuracies:
        valMatheString+='{'+str(accData)+','+str(valHSAccuracies[accData][0]/valHSAccuracies[accData][1])+'},'
    valMatheString=valMatheString[0:len(valMatheString)-1]
     
    valMatheStringTopOne='{'
    for accData in valHSAAccuraciesTopOne:
        valMatheStringTopOne+='{'+str(accData)+','+str(valHSAAccuraciesTopOne[accData][0]/valHSAAccuraciesTopOne[accData][1])+'},'
    valMatheStringTopOne=valMatheStringTopOne[0:len(valMatheStringTopOne)-1]+"}"
     
    print('Validation accuracy', correct/attempts)
    print('Validation user accuracy', totalUserAcc/len(valUsersAccuracies))
    print('Validation top one', topOneCorrect/attempts)
    print('Validation user accuracy top one', totalUserAccTopOne/len(valUsersAccuraciesTopOne))
    
    print('non hist guesses ', nonHistGuesses)
    
    print(valMatheStringTopOne)

result=loadData("/home/willie/workspace/MLKeras/heuristicsDatatrainAllFixedSrc")
trainUserInOut=result[0]

inputLen=result[1]
outputLen=result[2]        
print('inputLen', inputLen)
print('outputLen', outputLen)

userTrainRecs=loadRecs("/home/willie/workspace/MLKeras/heuristicsRecstrainAllFixedSrc")
   
validateUserInOut=loadData("/home/willie/workspace/MLKeras/heuristicsDatavalidateAllFixedSrc")[0]
userValidateRecs=loadRecs("/home/willie/workspace/MLKeras/heuristicsRecsvalidateAllFixedSrc")

userHistSizes=readHistSizes("/home/willie/workspace/MLKeras/heuristicsHistSizesvalidateAllFixedSrc")
           
# cut the text in semi-redundant sequences of maxlen characters
maxLen=20
step = 1

inputs=[]
outputs=[]
trainRecs=[]

validationInputs=[]
validationOutputs=[]
validationRecs=[]
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
        trainRecs+=[userTrainRecs[userData][i-1]]
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
        validationInputToUser[len(validationOutputs)]=userData
        validationIndToHistSize+=[userHistSizes[userData][i-1]]
        validationOutputs+=[validateUserInOut[userData]['out'][i-1]]
        validationRecs+=[userValidateRecs[userData][i-1]]
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

numHeurs=5
topN=3

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

# build the model: 2 stacked LSTM
print('Build model...')
model = Sequential()
model.add(LSTM(15, return_sequences=True, input_shape=(maxLen, inputLen), consume_less="mem"))
model.add(LSTM(15, return_sequences=False, consume_less="mem"))
model.add(Dense(15))
model.add(Activation('relu'))
model.add(Dropout(0.2))
model.add(Dense(outputLen))
model.add(Activation('softmax'))
model.load_weights('/home/willie/workspace/MLKeras/savedModels/AllFixedTop315Small12kBSrcPred29 30.h5')
model.compile(loss='categorical_crossentropy', optimizer='rmsprop')
plot(model, to_file='model.png')
# getTrainProgress(model, X, y, trainRecs, valX, validationOutputs, validationRecs, validationInputToUser, validationIndToHistSize)
# for iteration in range(0, 20):
#     print()
#     print('-' * 50)
#     print('Iteration', iteration)
#     model.fit(X, inOuts, batch_size=5000, nb_epoch=1, verbose=0)
#     if(iteration%5==0):
#         getTrainProgress(model, X, y, trainRecs, valX, validationOutputs, validationRecs, validationInputToUser, validationIndToHistSize)

#train the model, output generated text after each iteration



# getTrainProgress(model, X, y, trainRecs, valX, validationOutputs, validationRecs, validationInputToUser, validationIndToHistSize)
# for iteration in range(0, 1000):
#     print()
#     print('-' * 50)
#     print('Iteration', iteration)
#     history=model.fit(X, y, batch_size=5000, nb_epoch=1, verbose=0)
#     print(history)
#     
#     if(iteration%1==0):
#         getTrainProgress(model, X, y, trainRecs, valX, validationOutputs, validationRecs, validationInputToUser, validationIndToHistSize)
#         model.save_weights('/home/willie/workspace/MLKeras/savedModels/AllFixedTop315Small12kBSrcPred'+str(iteration)+' 30.h5', overwrite=True)

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

