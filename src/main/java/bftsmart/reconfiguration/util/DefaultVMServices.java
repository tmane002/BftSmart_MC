/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bftsmart.reconfiguration.util;

import bftsmart.reconfiguration.VMServices;
import bftsmart.tom.util.KeyLoader;

/**
 *
 * @author joao
 */
public class DefaultVMServices extends VMServices {

    public DefaultVMServices()
    {
        super();
    }


    public DefaultVMServices(KeyLoader keyLoader, String config_id) {
        super(keyLoader, config_id);
    }

    public static void main(String[] args) throws InterruptedException {

        System.out.println("args.length is "+args.length);

        if(args.length == 1){
            System.out.println("####Tpp Service[Disjoint]####");

            int smartId = Integer.parseInt(args[0]);
            
            (new DefaultVMServices()).removeServer(smartId);
            
                
        }else if(args.length == 5){
            System.out.println("####Tpp Service[Join]####");

            int smartId = Integer.parseInt(args[0]);
            String ipAddress = args[1];
            int port = Integer.parseInt(args[2]);
            int portRR = Integer.parseInt(args[3]);
            String config_id = args[4];

            (new DefaultVMServices(null, config_id)).addServer(smartId, ipAddress, port, portRR);
//            (new DefaultVMServices()).addServer(smartId, ipAddress, port, portRR);

        }else{
            System.out.println("Usage: java -jar TppServices <smart id> [ip address] [port]");
            System.exit(1);
        }

        Thread.sleep(2000);//2s
        

        System.exit(0);
    }
}
