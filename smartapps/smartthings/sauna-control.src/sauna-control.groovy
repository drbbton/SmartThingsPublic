/**
 *  Copyright 2015 SmartThings
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
 *  Curling Iron
 *
 *  Author: SmartThings
 *  Date: 2013-03-20
 */
definition(
    name: "Sauna Control",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Turns sauna on for pre-defined time to pre-heat, then shuts off.",
    category: "Convenience",
    iconUrl: "http://cdn.flaticon.com/png/512/34894.png",
    iconX2Url: "http://cdn.flaticon.com/png/512/34894.png"
)

preferences {
	section("Turn on this Sauna") {
		input name: "outlets", title: "Which?", type: "capability.switch", multiple: true
	}
	section("Shutoff pre-heating after this time") {
		input name: "minutes", title: "Minutes?", type: "number", multiple: false
	}
    section("How many minutes before shutoff should I Warn?") {
		input name: "warnminutes", title: "Minutes?", type: "number", multiple: false
	}
    section("What should we watch for your arrival?") {
		input "motionsensor", "capability.motionSensor", title: "Where?", multiple: false
	}
    section("How long after you leave should I shut it off?") {
		input name: "scheduledTurnOffMotion", title: "Minutes?", type: "number", multiple: false
	}
     section("What temp is the Sauna ready?") {
		input "temperature1", "number", title: "Temperature?"
	}
    section("What temp sensor should I watch for?") {
		input "temperatureSensor1", "capability.temperatureMeasurement", title: "Where?", multiple: false
	}
   
    
}
def installed()
{
	subscribe(app, appTouch)
    subscribe(motionsensor, "motion.active", motionActiveHandler)
    subscribe(temperatureSensor1, "temperature", temperatureHandler)
}

def updated()
{
	unsubscribe()
	subscribe(app, appTouch)
    subscribe(motionsensor, "motion.active", motionActiveHandler)
    subscribe(temperatureSensor1, "temperature", temperatureHandler)
}
def appTouch(evt) {
	log.debug "appTouch: $evt"
	outlets.on()
    def delay = (minutes*60)
    def delaywarn = ((minutes-warnminutes) * 60)
	runIn(delaywarn, "scheduledTurnOffwarn")
    runIn(delay, "scheduledTurnOff")

}

def motionActiveHandler(evt) {
log.debug "Motion detected, keep sauna on for ${scheduledTurnOffMotion} minutes."
unschedule("scheduledTurnOffwarn")
runIn((scheduledTurnOffMotion*60),scheduledTurnOff)
}

def scheduledTurnOff() {
	outlets.off()
	unschedule("scheduledTurnOff") // Temporary work-around to scheduling bug
}
def scheduledTurnOffwarn() {
	def outletsstate = outlets.currentState
    if (outletsstate == "on" ) {
    
    if (sendPush) {
        sendPush("Your sauna will shut off in ${warnminutes} minutes!")
    }}
	unschedule("scheduledTurnOffwarn") // Temporary work-around to scheduling bug
    
}


def temperatureHandler(evt) {
	log.trace "temperature: $evt.value, $evt"

	def saunareadytemp = temperature1
  if (evt.doubleValue >= saunareadytemp){  
			log.debug "Sauna Temp at $saunareadytemp:  You should use sauna"
			send("Your sauna is now hot: ${evt.value}${evt.unit?:"F"}")
            unsubscribe(temperatureSensor1)
}
}
private send(msg) {
        if (sendPushMessage != "No") {
            log.debug("sending push message")
            sendPush(msg)
        }
    

    log.debug msg
}