import matplotlib.pyplot as plt
import numpy as np
from numpy import genfromtxt

data = genfromtxt('/home/willie/workspace/Globus/data/heatmapAmtData.csv', delimiter=',')
fig, ax = plt.subplots()
heatmap = ax.pcolor(data, cmap=plt.cm.viridis)

# put the major ticks at the middle of each cell
ax.set_xticks(np.arange(data.shape[0])+0.5, minor=False)
ax.set_yticks(np.arange(data.shape[1])+0.5, minor=False)

# want a more natural, table-like display
ax.invert_yaxis()
ax.xaxis.tick_top()

plt.show()