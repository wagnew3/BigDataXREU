import numpy as np
import matplotlib.pyplot as plt

def readLongs(csv):
    numRead=0
    longs=[]
    csvFile=open(csv)
    lines=csvFile.read().splitlines()
    splitLines=[]
    for line in lines:
        longs+=[long(line)]
        numRead+=1
        if numRead>100000:
            break
    csvFile.close()
    return longs

x = np.matrix(readLongs("/home/willie/workspace/Globus/data/amounts"))
y = np.matrix(readLongs("/home/willie/workspace/Globus/data/throughputs"))


fig = plt.figure()
ax = plt.gca()
ax.scatter(x, y)
ax.set_yscale('log')
ax.set_xscale('log')
plt.show()