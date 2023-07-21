/**
 * Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bftsmart.demo.ycsb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.TreeMap;
import java.util.concurrent.*;

import bftsmart.benchmark.ThroughputLatencyClient;
import bftsmart.demo.counter.ClusterInfo;
import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceProxy;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Marcel Santos
 *
 */
public class YCSBServer extends DefaultRecoverable {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final boolean _debug = false;
    private TreeMap<String, YCSBTable> mTables;

    private boolean logPrinted = false;

    private int sid;

//    private ServiceProxy[] ServiceTaskArr;
////    private Task[] ServiceTaskArr;
//
    private ClusterInfo cinfo;

    private ThreadPoolExecutor executor;

    private Task[] tempTasks;

    private CountDownLatch latch;


    private long numRequests = 0;
    private double maxThroughput;
    private long startTime = 0;

    public static void main(String[] args) throws Exception {
        if (args.length == 1) {
            new YCSBServer(Integer.parseInt(args[0]));
        } else {
            System.out.println("Usage: java ... YCSBServer <replica_id>");
        }
    }

    private YCSBServer(int id) {

        this.cinfo = new ClusterInfo();

        this.sid = id;
        this.mTables = new TreeMap<>();

        System.out.println("config+cinfo.getClusterNumber(id) is: "+"config"+cinfo.getClusterNumber(id));



        new ServiceReplica(id, this, this,
                "config"+cinfo.getClusterNumber(id));

//        new ServiceReplica(id, this, this,
//                "config"+cinfo.getClusterNumber(id));


    }



    private void printMeasurement() {
        long currentTime = System.nanoTime();
        double deltaTime = (currentTime - startTime) / 1_000_000_000.0;
        if ((int) (deltaTime / 2) > 0) {
            long delta = currentTime - startTime;
            double throughput = numRequests / deltaTime;
            if (throughput > maxThroughput)
                maxThroughput = throughput;
            logger.info("M:(currentTime, requests[#]|delta[ns]|throughput[ops/s], max[ops/s])>({}|{}|{}|{}|{})",
                    currentTime, numRequests, delta, throughput, maxThroughput);
            numRequests = 0;
            startTime = currentTime;
        }
    }

    @Override
    public byte[][] appExecuteBatch(byte[][] commands, MessageContext[] msgCtx, boolean fromConsensus) {
        byte[][] replies = new byte[commands.length][];
        int index = 0;



        for (byte[] command : commands) {
            if (msgCtx != null && msgCtx[index] != null && msgCtx[index].getConsensusId() % 1000 == 0 && !logPrinted) {
                System.out.println("YCSBServer executing CID: " + msgCtx[index].getConsensusId());
                logPrinted = true;
            } else {
                logPrinted = false;
            }

            YCSBMessage aRequest = YCSBMessage.getObject(command);
            YCSBMessage reply = YCSBMessage.newErrorMessage("");
            if (aRequest == null) {
                replies[index] = reply.getBytes();
                continue;
            }
            if (_debug) {
                System.out.println("[INFO] Processing an ordered request");
            }
            switch (aRequest.getType()) {
                case CREATE: { // ##### operation: create #####
                    switch (aRequest.getEntity()) {
                        case RECORD: // ##### entity: record #####
                            if (!mTables.containsKey(aRequest.getTable())) {
                                mTables.put((String) aRequest.getTable(), new YCSBTable());
                            }
                            if (!mTables.get(aRequest.getTable()).containsKey(aRequest.getKey())) {
                                mTables.get(aRequest.getTable()).put(aRequest.getKey(), aRequest.getValues());
                                reply = YCSBMessage.newInsertResponse(0);
                            }
                            break;
                        default: // Only create records
                            break;
                    }
                    break;
                }

                case UPDATE: { // ##### operation: update #####
                    switch (aRequest.getEntity()) {
                        case RECORD: // ##### entity: record #####
                            if (!mTables.containsKey(aRequest.getTable())) {
                                mTables.put((String) aRequest.getTable(), new YCSBTable());
                            }
                            mTables.get(aRequest.getTable()).put(aRequest.getKey(), aRequest.getValues());
                            reply = YCSBMessage.newUpdateResponse(1);
                            break;
                        default: // Only update records
                            break;
                    }
                    break;
                }
            }
            if (_debug) {
                System.out.println("[INFO] Sending reply");
            }



//            this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
//
//            for (int i=0; i < this.cinfo.nClusters;i++)
//            {
//
//                if (i!=this.cinfo.clusterMap.get(this.sid) && this.sid==msgCtx[index].getLeader())
//                {
//                    System.out.println("i, this.sid, this.cinfo.clusterMap.get(this.sid), msgCtx[index].getLeader()"+
//                            i+ ", " +this.sid + ", " + this.cinfo.clusterMap.get(this.sid) + ", "+ msgCtx[index].getLeader());
//                    this.ServiceTaskArr[i] = new Task(this.sid, "config"+Integer.toString(i));
//
//                    this.ServiceTaskArr[i].setMsg(aRequest);
//                    this.executor.execute(this.ServiceTaskArr[i]);
//                }
//            }

//            if (this.cinfo.nClusters > 1)
//            {
//
//                this.tempTasks = new Task[this.cinfo.nClusters];
//                this.latch = new CountDownLatch(this.cinfo.nClusters-1);
//                for (int i=0; i < this.cinfo.nClusters;i++)
//                {
//
//                    if (this.cinfo.clusterMap.get(this.sid)==0 && this.sid==msgCtx[index].getLeader())
//                    {
//
//                        this.tempTasks[i] = new Task(this.sid, "config"+Integer.toString(i), latch);
//                        this.tempTasks[i].setMsg(aRequest);
//
//                        this.tempTasks[i].start();
//
//                    }
//                }
//
//                new Thread(() -> {
//                    try {
//                        latch.await();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }).start();
//            }




//            for (int i=0; i < this.cinfo.nClusters;i++)
//            {
//                if (i!=this.cinfo.clusterMap.get(this.sid) && this.sid==msgCtx[index].getLeader())
//                {
//                    this.ServiceTaskArr[i].invokeOrderedNoReply(aRequest.getBytes());
//                }
//            }




//            this.executor.shutdown();
//            try {
//                this.executor.awaitTermination(1000, TimeUnit.SECONDS);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }





            replies[index++] = reply.getBytes();


            //  send message to all other servers here






        }
//		System.out.println("RETURNING REPLY");




        return replies;
    }








    @Override
    public byte[] appExecuteUnordered(byte[] theCommand, MessageContext theContext) {
        YCSBMessage aRequest = YCSBMessage.getObject(theCommand);
        YCSBMessage reply = YCSBMessage.newErrorMessage("");
        if (aRequest == null) {
            return reply.getBytes();
        }
        if (_debug) {
            System.out.println("[INFO] Processing an unordered request");
        }

        switch (aRequest.getType()) {
            case READ: { // ##### operation: read #####
                switch (aRequest.getEntity()) {
                    case RECORD: // ##### entity: record #####
                        if (!mTables.containsKey(aRequest.getTable())) {
                            reply = YCSBMessage.newErrorMessage("Table not found");
                            break;
                        }
                        if (!mTables.get(aRequest.getTable()).containsKey(aRequest.getKey())) {
                            reply = YCSBMessage.newErrorMessage("Record not found");
                            break;
                        } else {
                            reply = YCSBMessage.newReadResponse(mTables.get(aRequest.getTable()).get(aRequest.getKey()), 0);
                            break;
                        }
                }
            }
        }
        if (_debug) {
            System.out.println("[INFO] Sending reply");
        }
        return reply.getBytes();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void installSnapshot(byte[] state) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(state);
            ObjectInput in = new ObjectInputStream(bis);
            mTables = (TreeMap<String, YCSBTable>) in.readObject();
            in.close();
            bis.close();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[ERROR] Error deserializing state: "
                    + e.getMessage());
        }
    }

    @Override
    public byte[] getSnapshot() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(mTables);
            out.flush();
            bos.flush();
            out.close();
            bos.close();
            return bos.toByteArray();
        } catch (IOException ioe) {
            System.err.println("[ERROR] Error serializing state: "
                    + ioe.getMessage());
            return "ERROR".getBytes();
        }
    }
}