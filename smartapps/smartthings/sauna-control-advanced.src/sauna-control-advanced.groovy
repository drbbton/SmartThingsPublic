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
definition(
    name: "Sauna Control - Advanced",
    namespace: "smartthings",
    author: "Blake Bristow",
    description: "Turns sauna on for pre-defined time to pre-heat, then shuts off.",
    category: "Convenience",
    iconUrl: "http://cdn.flaticon.com/png/512/34894.png",
    iconX2Url: "http://cdn.flaticon.com/png/512/34894.png"
)

preferences {
    section("Which one is the Sauna?") {
		input name: "outlets", title: "Which?", type: "capability.switch", multiple: false
	}
	section("Shutoff pre-heating after this time") {
		input name: "minutes", title: "Minutes?", type: "number", multiple: false
	}
    section("How many minutes before auto-shutoff should I Warn?") {
		input name: "warnminutes", title: "Minutes?", type: "number", multiple: false
	}
    section("What should we watch for your arrival?") {
		input "motionsensor", "capability.motionSensor", title: "Where?", multiple: false
	}
     section("Do you have a primary light in the Sauna?") {
		input name: "saunalight1", title: "Which?", type: "capability.switch", multiple: true, required: false
	}
    section("How long after you leave should I heat for") {
		input name: "scheduledTurnOffMotion", title: "Minutes?", type: "number", multiple: false
	}
     section("What temp is the Sauna ready?") {
		input "temperature1", "number", title: "Temperature?"
	}
    section("What temp sensor should I watch for?") {
		input "temperatureSensor1", "capability.temperatureMeasurement", title: "Where?", multiple: false
	}
     section("Do you have a light I can tell you the heat status?") {
		input name: "saunastatuslight", title: "Which?", type: "capability.colorControl", multiple: true, required: false
	}
   
}
def installed()
{
subscribe(motionsensor, "motion.active", motionActiveHandler)
    subscribe(temperatureSensor1, "temperature", temperatureHandler)
    subscribe(outlets, "switch.on", switchHandlerOn)
    subscribe(outlets, "switch.off", switchHandlerOff)
}

def updated()
{
	unsubscribe() 
	subscribe(motionsensor, "motion.active", motionActiveHandler)
    subscribe(temperatureSensor1, "temperature", temperatureHandler)
    subscribe(outlets, "switch.on", switchHandlerOn)
    subscribe(outlets, "switch.off", switchHandlerOff)
}


def switchHandlerOn(evt) {
	log.debug "switchHandler: $evt"
    
    saunalight1.setLevel(10)
    saunalight1.on()
    saunastatuslight.setColor(hue: 11, saturation: 6, level:100)
    saunastatuslight.on()
    def delay = (minutes*60)
    def delaywarn = ((minutes-warnminutes) * 60)
	runIn(delaywarn, "scheduledTurnOffwarn")
    runIn(delay, "scheduledTurnOff")

}


def motionActiveHandler(evt) {
log.debug "Motion detected, keep sauna on for ${scheduledTurnOffMotion} minutes."
unschedule("scheduledTurnOffwarn")
saunalight1.setLevel(50)
runIn((scheduledTurnOffMotion*60),scheduledTurnOff)
}

def scheduledTurnOff() {
	outlets.off()
    saunalight1.off()
    saunastatuslight.off()
	unschedule("scheduledTurnOff") // Temporary work-around to scheduling bug
    unsubscribe(temperatureSensor1)
    unsubscribe(motionsensor)
}

def switchHandlerOff(evt) {
    saunalight1.off()
    saunastatuslight.off()
	unschedule("scheduledTurnOff") // Temporary work-around to scheduling bug
    unsubscribe(temperatureSensor1)
    unsubscribe(motionsensor)
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
//Set the color of the light based on the temperature of the sauna
log.debug "In send color"
	def curtemp = evt.doubleValue
	def hueColor = 47
    def saturation = 100
    def saunalighttempdesc = "none"
    def saunareadytemp = temperature1
        if (curtemp < (saunareadytemp*(60/100)) ) {
        hueColor = 61
        saunalighttempdesc = "Deep Blue"
		}
		else if (curtemp>=(saunareadytemp*(60/100)) && curtemp<(saunareadytemp*(70/100))) {
            hueColor = 47
            saunalighttempdesc = "Turquoise"
		}
		else if (curtemp>=(saunareadytemp*(70/100)) && curtemp<(saunareadytemp*(80/100))) {
            hueColor = 33
            saunalighttempdesc = "Green"
		}
		else if (curtemp>=(saunareadytemp*(80/100)) && curtemp<(saunareadytemp*(90/100))) {
            hueColor = 17
            saunalighttempdesc = "Yellow"
		}
		else if (curtemp>=(saunareadytemp*(90/100)) && curtemp<saunareadytemp) {
            hueColor = 7
            saunalighttempdesc = "Orange"
		}
		else if(curtemp>=saunareadytemp) {
            hueColor = 0
            saunalighttempdesc = "Red"
		}
		
	//Change the color of the light
	def saunalighttemp = [hue: hueColor, saturation: saturation, level: 100]  
	saunastatuslight.setColor(saunalighttemp)
        log.debug "$app.label: Temp is: $curtemp Setting Color = $saunalighttempdesc"

	
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