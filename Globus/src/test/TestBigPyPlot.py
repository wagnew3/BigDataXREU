import numpy as np
import matplotlib.pyplot as plt
import random

nsteps = 10000000
draws = np.random.randint(0,2,size=nsteps)
steps = np.where(draws>0,1,-1)
walk = steps.cumsum()
plt.plot(np.arange(nsteps), np.array(walk))
plt.title("Big Set Random Walk with $\pm1$ steps")
plt.show()