#!/usr/bin/env python
# coding: utf-8

# In[1]:


import numpy as np
import os
import shutil


# In[2]:


for folder in os.listdir('.'):
    if 'config' in folder and folder!='config' and 'zip' not in folder and '.' not in folder:
        shutil.rmtree(folder)


# In[3]:


total_count = 8


replica_count = 4

n_clusters = int(total_count / replica_count)


# In[4]:


clusterIpMap = {}

clusterIpMap[0] = '172.17.0.2'
clusterIpMap[1] = '172.17.0.3'
clusterIpMap[2] = '172.17.0.4'
clusterIpMap[3] = '172.17.0.5'
clusterIpMap[4] = '172.17.0.7'
clusterIpMap[5] = '172.17.0.8'
clusterIpMap[6] = '172.17.0.9'
clusterIpMap[7] = '172.17.0.10'
clusterIpMap[8] = '172.17.0.11'
clusterIpMap[9] = '172.17.0.12'
clusterIpMap[10] = '172.17.0.13'
clusterIpMap[11] = '172.17.0.14'
clusterIpMap[12] = '172.17.0.15'
clusterIpMap[13] = '172.17.0.16'
clusterIpMap[14] = '172.17.0.17'
clusterIpMap[15] = '172.17.0.18'
clusterIpMap[7001] = '172.17.0.6'



clusterPortMap1 = {}
clusterPortMap2 = {}

for i in range(total_count):
    
    clusterPortMap1[i] = str(11000+i*10)
    
for i in range(total_count):
    clusterPortMap2[i] = str(11000+i*10+1)
    
clusterPortMap1[7001] = '11100'
clusterPortMap2[7001] = '11100'


# In[5]:


clusterPortMap1, clusterPortMap2


# In[6]:


for i in range(16):
    os.system('./runscripts/smartrun.sh bftsmart.tom.util.RSAKeyPairGenerator '+str(i)+' 1024')
    
os.system('./runscripts/smartrun.sh bftsmart.tom.util.RSAKeyPairGenerator '+str(7001)+' 1024')


# In[7]:


for i in range(n_clusters):
    
    if os.path.exists('config'+str(i)):
        shutil.rmtree('config'+str(i))
    
    os.mkdir('config'+str(i))
    os.mkdir('config'+str(i)+'/keysRSA')
    os.mkdir('config'+str(i)+'/keysSunEC')
    os.mkdir('config'+str(i)+'/keysECDSA')
    
    
    shutil.copytree('config/workloads/', 'config'+str(i)+'/workloads')
    shutil.copytree('config/keysSSL_TLS/', 'config'+str(i)+'/keysSSL_TLS')
    
    
    for file in os.listdir('config/keys/'):
    
        shutil.copy('config/keys/'+file, 'config'+str(i)+'/keysRSA/'+file)
        shutil.copy('config/keys/'+file, 'config'+str(i)+'/keysSunEC/'+file)
        shutil.copy('config/keys/'+file, 'config'+str(i)+'/keysECDSA/'+file)
    
    
    shutil.copy('config/system.config', 'config'+str(i)+'/system.config')    
    shutil.copy('config/logback.xml', 'config'+str(i)+'/logback.xml')
    
    
    
    


# In[8]:


for n_cluster in range(n_clusters):
    
    with open('config'+str(n_cluster)+'/hosts.config','w') as file:
        
        for i_replica in range(replica_count):
            file.writelines(str(i_replica)+' '+clusterIpMap[n_cluster*replica_count+i_replica]+' '  +clusterPortMap1[n_cluster*replica_count+i_replica]+' ' +  clusterPortMap2[n_cluster*replica_count+i_replica]+'\n')
        file.writelines('7001 '+clusterIpMap[7001]+' '+clusterPortMap1[7001]+'\n')
        file.close()
        


# In[9]:


for n_cluster in range(n_clusters):
    with open('config'+str(n_cluster)+'/system.config','r') as file:
        data = file.readlines()
        f = int((replica_count-1)/3)
        
        for n_line in range(len(data)):
            if ('system.servers.num' in data[n_line]) and '#' not in data[n_line]:
                
                data[n_line] = 'system.servers.num = '+str(replica_count)
                
            if ('system.servers.f' in data[n_line]) and '#' not in data[n_line]:
                data[n_line] = 'system.servers.f = '+str(f)
                
            if ('system.initial.view' in data[n_line]) and '#' not in data[n_line]:
                data[n_line] = 'system.initial.view = '
                for iter_view in range(replica_count):
                    data[n_line] = data[n_line] +str(iter_view)+','
                
                data[n_line] = data[n_line][:-1]
                
                
        file.close()
        
    with open('config'+str(n_cluster)+'/system.config', 'w') as f:
        for line in data:
            f.write(f"{line}\n")
        


# In[10]:


for n_cluster in range(n_clusters):
    for i in range(11):
        print('config'+str(n_cluster)+'/keysRSA/'+'publickey7001',                     'config'+str(n_cluster)+'/keysRSA/'+'publickey'+str(1000+i))
        shutil.copy('config'+str(n_cluster)+'/keysRSA/'+'publickey7001',                     'config'+str(n_cluster)+'/keysRSA/'+'publickey'+str(1000+i))
        shutil.copy('config'+str(n_cluster)+'/keysRSA/'+'privatekey7001',                     'config'+str(n_cluster)+'/keysRSA/'+'privatekey'+str(1000+i))

