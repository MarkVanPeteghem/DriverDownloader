n = 0

for i in xrange(1, 100):
	for j in xrange(1, 1000):
		for k in xrange(1, 1000):
			n = (n+i)*j%k
	file = open("file%d.txt" % i, "w")
	file.write("%d" % n)
	file.close()
