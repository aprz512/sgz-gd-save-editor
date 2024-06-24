extends Node

func _ready():
	var skills = {
			"LV1":"智迟", 
			"LV2":"", 
			"LV3":"藤甲", 
			"LV4":"", 
			"LV5":"白毦", 
			"LV6":"", 
			"LV7":"玄阵", 
			"LV8":""
		};
	var skillsData = JSON.stringify(skills).to_utf8_buffer()
	print(PackedByteArray(skillsData))