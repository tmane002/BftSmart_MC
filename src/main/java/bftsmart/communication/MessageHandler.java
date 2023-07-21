/**
Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package bftsmart.communication;

import bftsmart.consensus.messages.OtherClusterMessage;
import bftsmart.tom.leaderchange.LCMessageOCReply;
import bftsmart.tom.leaderchange.LCMessageOtherCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bftsmart.consensus.messages.ConsensusMessage;
import bftsmart.consensus.roles.Acceptor;
import bftsmart.statemanagement.SMMessage;
import bftsmart.tom.core.TOMLayer;
import bftsmart.tom.core.messages.ForwardedMessage;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.leaderchange.LCMessage;
import bftsmart.tom.util.TOMUtil;

import java.io.IOException;

/**
 *
 * @author edualchieri
 */
public class MessageHandler {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private Acceptor acceptor;
	private TOMLayer tomLayer;

	public MessageHandler() {}

	public void setAcceptor(Acceptor acceptor) {
		this.acceptor = acceptor;
	}

	public void setTOMLayer(TOMLayer tomLayer) {
		this.tomLayer = tomLayer;
	}

	@SuppressWarnings("unchecked")
	protected void processData(SystemMessage sm) throws IOException, ClassNotFoundException {

//		logger.info("SystemMessage being processed inside processData");

		if (sm instanceof OtherClusterMessage)
		{
//			logger.info("\n\n\n PROCESSING OtherClusterMessage by Server Communication system");
			tomLayer.requestReceivedOtherClusters((OtherClusterMessage) sm);
			return;
		}

		if (sm instanceof LCMessageOCReply)
		{
			tomLayer.signalWaitingForLCMessageOCReply();
		}



		if (sm instanceof ConsensusMessage) {
			logger.info("ConsensusMessage being processed MessageHandler");

			int myId = tomLayer.controller.getStaticConf().getProcessId();

			ConsensusMessage consMsg = (ConsensusMessage) sm;

			if (consMsg.authenticated || consMsg.getSender() == myId)
				acceptor.deliver(consMsg);
			else {
				logger.info("Discarding unauthenticated message from " + sm.getSender());
			}

		}

//
		else {
			if (sm.authenticated) {
				/*** This is Joao's code, related to leader change */
				if (sm instanceof LCMessage) {

					LCMessage lcMsg = (LCMessage) sm;
					logger.info("LCMessage being processed inside processData: lcMsg.getSender(), lcMsg.getType(), lcMsg.TRIGGER_LC_LOCALLY are {}, {} and {}",
							lcMsg.getSender(), lcMsg.getType(), lcMsg.TRIGGER_LC_LOCALLY);
					String type = null;
					switch (lcMsg.getType()) {

					case TOMUtil.STOP:
						type = "STOP";
						break;
					case TOMUtil.STOPDATA:
						type = "STOPDATA";
						break;
					case TOMUtil.SYNC:
						type = "SYNC";
						break;
					default:
						type = "LOCAL";
						break;
					}

					if (lcMsg.getReg() != -1 && lcMsg.getSender() != -1)
						logger.info("Received leader change message of type {} " + "for regency {} from replica {}",
								type, lcMsg.getReg(), lcMsg.getSender());
					else
						logger.info("Received leader change message from myself");
					
					if (lcMsg.TRIGGER_LC_LOCALLY)
					{
						logger.info("Supposed to do Leader change");

						tomLayer.requestsTimer.run_lc_protocol();
						logger.info("------Leader Change Protocol DONE???");
					}
					else
						tomLayer.getSynchronizer().deliverTimeoutRequest(lcMsg);
					/**************************************************************/

				}


				else if (sm instanceof ForwardedMessage) {
					logger.info("ForwardedMessage received");
					TOMMessage request = ((ForwardedMessage) sm).getRequest();
					tomLayer.requestReceived(request, false);//false -> message was received from a replica -> do not drop it

					/** This is Joao's code, to handle state transfer */
				} else if (sm instanceof SMMessage) {
//					logger.info("SystemMessage being processed inside processData 4");
					SMMessage smsg = (SMMessage) sm;
					switch (smsg.getType()) {
					case TOMUtil.SM_REQUEST:
						logger.info("SMRequestDeliver activated");
						tomLayer.getStateManager().SMRequestDeliver(smsg, tomLayer.controller.getStaticConf().isBFT());
//						tomLayer.resendOCMD();
						break;
					case TOMUtil.SM_REPLY:
						logger.info("SMReplyDeliver activated with state: {}", smsg.getState());

						tomLayer.getStateManager().SMReplyDeliver(smsg, tomLayer.controller.getStaticConf().isBFT());
						break;
					case TOMUtil.SM_ASK_INITIAL:
						logger.info("currentConsensusIdAsked activated");

						tomLayer.getStateManager().currentConsensusIdAsked(smsg.getSender(), smsg.getCID());
						break;
					case TOMUtil.SM_REPLY_INITIAL:
						logger.info("currentConsensusIdReceived activated");

						tomLayer.getStateManager().currentConsensusIdReceived(smsg);
						break;
					case TOMUtil.NEW_NODE_READY:
							logger.info("SENT RECONFIG");
							tomLayer.signalReconfigConfirmationNewNode();
							break;
					case TOMUtil.REMOTE_VIEW_CHANGE:
						logger.info("REMOTE_VIEW_CHANGE receieved from {} with CID: {}",
								sm.getSender(), ((SMMessage) sm).getCID());
						tomLayer.StartLeaderChange(sm);
						break;
					case TOMUtil.REMOTE_NODE_READY:
						logger.info("REMOTE_NODE_READY receieved from {} with CID: {}",
								sm.getSender(), ((SMMessage) sm).getCID());
						tomLayer.signalRemoteChange(sm);
						break;
					default:
						logger.info("default getStateManager().stateTimeout() activated");

						tomLayer.getStateManager().stateTimeout();
						break;
					}
					/******************************************************************/
				} else {
					logger.warn("UNKNOWN MESSAGE TYPE: " + sm);
				}
			} else {
				logger.warn("Discarding unauthenticated message from " + sm.getSender());
			}
		}
	}

	protected void verifyPending() {
		tomLayer.processOutOfContext();
	}
}
