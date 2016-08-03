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
import theano
import threading
import time

class ValidateModel:
    
    lock=threading.Lock()
    
    inputLen=0
    outputLen=0
    validationInputs=[]
    validationOutputs=[]
    correct=0.0
    attempts=0.0
    
    finished=0
    maxLen=20
    
    def __init__(self):
        validateUserInOut={}
        tLines=self.readToLines("/home/willie/workspace/MLKeras/heuristicsDatavalidateTop3MCHistMUINSTUEP1mill")
        state=''
        userID=''
        for tLine in tLines:
            if tLine=='in':
                state='in'
            elif tLine=='out':
                state='out'
            elif tLine[0:5]=='user ':
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
                if state=='in':
                    self.inputLen=len(floats)
                else:
                    self.outputLen=len(floats)
                    
        # cut the text in semi-redundant sequences of maxlen characters
        inputs=[]
        outputs=[]
        
        for userData in validateUserInOut:
            for i in range(0, min(len(validateUserInOut[userData]['in']), len(validateUserInOut[userData]['out']))):
                self.validationInputs+=[validateUserInOut[userData]['in'][i: min(len(validateUserInOut[userData]['in']), i+self.maxLen)]]
                self.validationOutputs+=[validateUserInOut[userData]['out'][min(len(validateUserInOut[userData]['out'])-1, i+self.maxLen)]]
        print('nb validation sequences:', len(self.validationInputs))
        
        numberThreads=1
             
        for threadInd in range(numberThreads):
            threading.Thread(target=self.validate).start()
             
        while self.finished<numberThreads:
            time.sleep(5)
            with self.lock:
                print("completed: ", self.attempts)
                if self.attempts>0: 
                    print(str(self.correct))
                    print(str(self.attempts))
                    print("Validation Accuracy: ",(self.correct/self.attempts))
        print("Final Validation Accuracy: ",(self.correct/self.attempts))

    def validate(self):
        # build the model: 2 stacked LSTM
        print('Load model...')
        model = Sequential()
        model.add(LSTM(256, return_sequences=True, input_shape=(self.maxLen, 51)))
        model.add(LSTM(256, return_sequences=False))
        model.add(Dropout(0.2))
        model.add(Dense(self.outputLen))
        model.add(Activation('softmax'))
        model.compile(loss='categorical_crossentropy', optimizer='rmsprop')
        model.load_weights("/home/willie/workspace/MLKeras/savedModels/Top3MCHistMUINSTUEP1millPred20.h5")
        
        while(True):
            with self.lock:
                if len(self.validationInputs)==0:
                    break
                validationInput=self.validationInputs.pop()
                validationOutput=self.validationOutputs.pop()
            x=np.zeros((1, self.maxLen, 51))
            x[0][0:len(validationInput)]=validationInput
            preds = model.predict(x, verbose=0)[0]
            predAnswerInd=np.argmax(preds)
            maxInds=[]
            maxVal=max(validationOutput)
            if maxVal>0:
                for ind in range(0, len(validationOutput)):
                    if validationOutput[ind]==maxVal:
                        maxInds+=[ind]
                if predAnswerInd in maxInds:
                    with self.lock:
                        self.correct+=1
            with self.lock:
                self.attempts+=1
        
        with self.lock:
            self.finished+=1;

    def readToLines(self, file):
        csvFile=open(file)
        lines=csvFile.read().splitlines()
        return lines

ValidateModel()