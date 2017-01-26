/**
  *
  *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
  *  in compliance with the License. You may obtain a copy of the License at:
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
  *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
  *  for the specific language governing permissions and limitations under the License.
  *
  */
 metadata {
 	definition (name: "Sauna Controller", namespace: "BlakeBristow", author: "blake@blizake.com") {
         capability "Refresh"
         capability "Polling"
         capability "Sensor"
         capability "Configuration"
         capability "Switch"
         
		 
        
         command "on1"
         command "off1"
         command "on2"
         command "off2"
         attribute "switch","ENUM", ["on","off"]
         attribute "side1switch","ENUM", ["on","off"]
         attribute "side2switch","ENUM", ["on","off"]
         
         
         


     	//fingerprint profileId: "0104", inClusters: "0000", outClusters: "000D,0006"
         fingerprint inClusters: "0000 0001 0003 0004 0005 0006", endpointId: "01", deviceId: "0100", profileId: "0104"

 	}

 	// simulator metadata
 	simulator {
     }

 	// UI tile definitions
 	tiles {


standardTile("switch", "device.switch", width: 3, height: 3, canChangeIcon: true) {
 			state "off", label: 'Off', action: "Switch.on", icon: "st.switches.switch.off", backgroundColor: "#d6ebff"
 			state "on", label: 'Heating', action: "Switch.off", icon: "st.switches.switch.on", backgroundColor: "#ff0000" 
            
 		}
 		standardTile("side1switch", "device.side1switch", width: 1, height: 1, canChangeIcon: true) {
 			state "off", label: 'Side1 ${name}',action: "on1", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState:"Sent"
 			state "on", label: 'Side1 ${name}', action: "off1", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState:"Sent" 
            state "Sent", label: 'wait', icon: "st.motion.motion.active", backgroundColor: "#ffa81e" 
 		}

 		standardTile("side2switch", "device.side2switch", width: 1, height: 1, canChangeIcon: true) {
 			state "off", label: 'Side2 ${name}', action: "on2", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState:"Sent"
 			state "on", label: 'Side2 ${name}', action: "off2", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState:"Sent"
            state "Sent", label: 'wait', icon: "st.motion.motion.active", backgroundColor: "#ffa81e" 
 		}

         standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat") {
 			state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
 		}

 		main (["switch"])
 		details (["switch", "side1switch", "side2switch", "refresh"])
 	}
 }

 // Parse incoming device messages to generate events
 def parse(String description) {
     log.debug "Parse description $description"
     def name = null
     def value = null

     if (description?.startsWith("catchall: 0104 0006 01")) {
         log.debug "On/Off command received from EP 1"
         if (description?.endsWith(" 01 0140 00 ${device.deviceNetworkId} 00 00 0000 0B 01 0000")){
         	name = "side1switch"
             value = "off"}
         else if (description?.endsWith(" 01 0140 00 ${device.deviceNetworkId} 00 00 0000 01 01 0000001000")){
         	name = "side1switch"
             value = "off"}
         else if (description?.endsWith(" 01 0140 00 ${device.deviceNetworkId} 00 00 0000 0B 01 0100")){
         	name = "side1switch"
             value = "on"} 
         else if (description?.endsWith(" 01 0140 00 ${device.deviceNetworkId} 00 00 0000 01 01 0000001001")){
         	name = "side1switch"
             value = "on"} 
     }  
     else if (description?.startsWith("catchall: 0104 0006 02")) {
         log.debug "On/Off command received from EP 2"    
         if (description?.endsWith(" 01 0140 00 ${device.deviceNetworkId} 00 00 0000 0B 01 0000")){
         	name = "side2switch"
             value = "off"}
         else if (description?.endsWith(" 01 0140 00 ${device.deviceNetworkId} 00 00 0000 01 01 0000001000")){
         	name = "side2switch"
             value = "off"}
         else if (description?.endsWith(" 01 0140 00 ${device.deviceNetworkId} 00 00 0000 0B 01 0100")){
         	name = "side2switch"
             value = "on"} 
         else if (description?.endsWith(" 01 0140 00 ${device.deviceNetworkId} 00 00 0000 01 01 0000001001")){
         	name = "side2switch"
             value = "on"} 
     }

 	def result = createEvent(name: name, value: value)
     log.debug "Parse returned ${result?.descriptionText}"
     return result
}
 // Commands to device

 def on() {
 	log.debug "Master on()"
    sendEvent(name: "switch", value: "on")
    //sendEvent(name: "side1switch", value: "on")
 	//sendEvent(name: "side2switch", value: "on")
    def cmd = []
    
     cmd << "st cmd 0x${device.deviceNetworkId} 0x01 0x0006 0x1 {}"	// send on for side1switch
     cmd << "delay 150"
     cmd << "st cmd 0x${device.deviceNetworkId} 0x02 0x0006 0x1 {}"	// Send on for side2switch
     
     cmd
  
 }

 def off() {
 	log.debug "Master off()"
    sendEvent(name: "switch", value: "off")
   // sendEvent(name: "side1switch", value: "off")
 	// sendEvent(name: "side2switch", value: "off")
    
      def cmd = []
    
     cmd << "st cmd 0x${device.deviceNetworkId} 0x01 0x0006 0x0 {}"	// send on for side1switch
     cmd << "delay 150"
     cmd << "st cmd 0x${device.deviceNetworkId} 0x02 0x0006 0x0 {}"	// Send on for side2switch
     
     cmd
 	
 }
 
 def on1() {
 	log.debug "Relay 1 on()"
 	//sendEvent(name: "side1switch", value: "on")
 	"st cmd 0x${device.deviceNetworkId} 0x01 0x0006 0x1 {}"
 }

 def off1() {
 	log.debug "Relay 1 off()"
 	//sendEvent(name: "side1switch", value: "off")
 	"st cmd 0x${device.deviceNetworkId} 0x01 0x0006 0x0 {}"
 }   
 def on2() {
 	log.debug "Relay 2 on()"
 	//sendEvent(name: "side2switch", value: "on")
 	"st cmd 0x${device.deviceNetworkId} 0x02 0x0006 0x1 {}"
 }

 def off2() {
 	log.debug "Relay 2 off()"
 	//sendEvent(name: "side2switch", value: "off")
 	"st cmd 0x${device.deviceNetworkId} 0x02 0x0006 0x0 {}"
 }

 def poll(){
 	log.debug "Poll is calling refresh"
 	refresh()
 }

 def refresh() {
 	log.debug "sending refresh command"
     def cmd = []

     cmd << "st rattr 0x${device.deviceNetworkId} 0x01 0x0006 0x0000"	// Read on / off value at End point 0x01 
     cmd << "delay 150"

     cmd << "st rattr 0x${device.deviceNetworkId} 0x02 0x0006 0x0000"	// Read on / off value at End point 0x02 

     cmd
 }



 def configure() {
 	log.debug "Binding SEP 0x01 and 0x02 DEP 0x01 Cluster 0x0006 On / Off cluster to hub" 
     def cmd = []
     cmd << "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {${device.zigbeeId}} {}"	// Bind on/off output to SmartThings hub for end point 1
     cmd << "delay 150"
     cmd << "zdo bind 0x${device.deviceNetworkId} 0x02 0x01 0x0006 {${device.zigbeeId}} {}" 	// Bind on/off output to SmartThings hub for end point 2
     cmd
 }