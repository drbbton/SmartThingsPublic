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
    section("Tell me about your Sauna:") {
		input name: "outlets", title: "Which one is your sauna heater?", type: "capability.switch", multiple: false
        input "motionsensor", "capability.motionSensor", title: "What motion do we watch for your arrival?", multiple: false
        input "temperatureSensor1", "capability.temperatureMeasurement", title: "What temp should we use for sauna?", multiple: false
	}
	section("Let's setup your Sauna") {
		input name: "minutes", title: "When turned on, how long should the Sauna pre-heat before turning off?", type: "number"
        input name:"scheduledTurnOffMotion", title: "How many minutes should I wait to make sure you are out of the Sauna?", type: "number", multiple: false
	}
    section( "Notifications" ) {
		input "sendPushMessage", "enum", title: "Send a push notification when Sauna's ready?", metadata:[values:["Yes","No"]], required: true
        input "temperature1", "number", title: "What temp is the Sauna ready?", defaultValue: "140"
        input name: "warnminutes", title: "How many minutes before pre-heating shuts off should I alert you?", type: "number", defaultValue: "15"
   }
     section("Sauna Lighting (Optional)") {
		input name:"saunalight", title: "Which is your main light?", type: "capability.switch", multiple: false, required: false, hideWhenEmpty: true
        input "saunalightlevel1", "enum", title: "How bright should the main light be at BEFORE you enter?", metadata:[values:["5","10","25","50","75","100"]], defaultValue: "5"
        input "saunalightlevel2", "enum", title: "How bright should the main light be at AFTER you enter?", metadata:[values:["5","10","25","50","75","100"]], defaultValue: "50"
        input name: "saunastatuslight", title: "Do you have a light to show heating progress?", type: "capability.colorControl", multiple: false, required: false, hideWhenEmpty: true
        input "saunastatuslightlevel", "enum", title: "How bright should the progress light be?", metadata:[values:["5","10","25","50","75","100"]], defaultValue: "50"
	}
     
   
}
def installed()
{

    subscribe(outlets, "switch.on", switchHandlerOn)
    subscribe(outlets, "switch.off", switchHandlerOff)
}

def updated()
{
	unsubscribe() 
    subscribe(outlets, "switch.on", switchHandlerOn)
    subscribe(outlets, "switch.off", switchHandlerOff)
}


def switchHandlerOn(evt) {
subscribe(motionsensor, "motion.active", motionActiveHandler)
subscribe(temperatureSensor1, "temperature", temperatureHandler)
	log.debug "switchHandler: $evt"
    saunalight.setLevel(saunalightlevel1)
    saunalight.on()
    saunastatuslight.setColor(hue: 11, saturation: 6, level: saunastatuslightlevel)
    saunastatuslight.on()
    def delay = (minutes*60)
    def delaywarn = ((minutes-warnminutes) * 60)
    state.saunaSmsSent = (1483296519)
	runIn(delaywarn, "scheduledTurnOffwarn")
    runIn(delay, "scheduledTurnOff")

}


def motionActiveHandler(evt) {
log.debug "Motion detected, keep sauna on for ${scheduledTurnOffMotion} minutes."
unschedule("scheduledTurnOffwarn")
saunalight.setLevel(saunalightlevel2)
runIn((scheduledTurnOffMotion*60),scheduledTurnOff)
}

def scheduledTurnOff() {
	outlets.off()
    saunalight.off()
    saunastatuslight.off()
	unschedule("scheduledTurnOff") // Temporary work-around to scheduling bug
    unschedule("scheduledTurnOffwarn") // Temporary work-around to scheduling bug
    unsubscribe(temperatureSensor1)
    unsubscribe(motionsensor)
    log.debug "Scheduled Shut off"
}

def switchHandlerOff(evt) {
    saunalight.off()
    saunastatuslight.off()
	unschedule("scheduledTurnOff") // Temporary work-around to scheduling bug
    unschedule("scheduledTurnOffwarn")
    unsubscribe(temperatureSensor1)
    unsubscribe(motionsensor)
}
def scheduledTurnOffwarn() {
	def outletsstate = outlets.currentState("switch").value
    log.debug "in scheduled turn off warning"
    if (outletsstate == "on" ) {
    log.debug "Sauna should shut off in $warnminutes"
        send("Your sauna will shut off in ${warnminutes} minutes!")
    }
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
	def saunalighttemp = [hue: hueColor, saturation: saturation, level: saunastatuslightlevel]  
	saunastatuslight.setColor(saunalighttemp)
        log.debug "$app.label: Temp is: $curtemp Setting Color = $saunalighttempdesc"

 if (evt.doubleValue >= saunareadytemp){ 
		log.debug "Checking to see if you have already been notified about the temp:<= $saunareadytemp"
def lastsms = state.saunaSmsSent
def timeSinceLastSend = (now() - lastsms)

if (timeSinceLastSend >= (1000*60*90))  {
 log.debug "Sauna Temp at $saunareadytemp:  You should use sauna. Sending Push"
            send("Your sauna is now ready: ${evt.value}${evt.unit?:"F"}")
            state.saunaSmsSent = now()
		} else 
   log.debug "Sauna Temp at $saunareadytemp:  Push already sent within 90 min or state value: $timeSinceLastSend ago."	
            }
}

private send(msg) {
        if (sendPushMessage != "No") {
            log.debug("sending push message")
            sendPush(msg)
        }
        else {
        log.debug "Sauna Temp at $saunareadytemp: Thou Push notifications are disabled" }
    log.debug msg
}