extends Node

func _ready():
	generate_skills_data()
	
func decrypt_skill_data(skills:String) -> String :
	var encrypted = PoolByteArray(hex_decode(skills))
	var sign_version = encrypted[encrypted.size() - 1]
	var signed_len = encrypted[encrypted.size() - 2]
	var signed = encrypted.subarray(encrypted.size() - signed_len - 2, encrypted.size() - 3)
	encrypted = encrypted.subarray(0, encrypted.size() - signed_len - 3)
	encrypted = hex_decode(encrypted.get_string_from_ascii())
	var decrypted = ECB_Decrypted(encrypted)
	print(decrypted)
	return decrypted.get_string_from_ascii()
	
func ECB_Encrypted(data:PoolByteArray)->PoolByteArray:
	var aes = AESContext.new()
	var key = "gd secret key!!!"
	while data.size() % 16 != 0:
		data.append(0)
	aes.start(AESContext.MODE_ECB_ENCRYPT, key.to_utf8())
	var encrypted:PoolByteArray = aes.update(data)
	aes.finish()
	return encrypted
	
func ECB_Decrypted(encrypted:PoolByteArray)->PoolByteArray:
	if encrypted.empty():
		return PoolByteArray([])
	var aes = AESContext.new()
	var key = "gd secret key!!!"
	
	aes.start(AESContext.MODE_ECB_DECRYPT, key.to_utf8())
	var decrypted:PoolByteArray = aes.update(encrypted)
	aes.finish()
	return decrypted

func hex_decode(data:String)->PoolByteArray:
	var ret = []
	var b:int = 0
	var odd = true
	for c in data.to_lower():
		if c in "abcdef":
			b += ord(c) - ord("a") + 10
		else :
			b += int(c)
		if odd:
			b *= 16
			odd = false
		else :
			ret.append(b)
			odd = true
			b = 0
	return ret

func generate_skills_data():
	var result_data = PoolByteArray([])
	var skills_data = get_skills();
	skills_data = ECB_Encrypted(skills_data).hex_encode().to_ascii()
	result_data.append_array(skills_data)
	var sign_data = crypto_sign_short(skills_data)
	result_data.append_array(sign_data)
	result_data.append(64)
	result_data.append(2)
	var hex_string = result_data.hex_encode()
	print(hex_string)
	
	
func get_skills()->PoolByteArray:
	return PoolByteArray([123, 34, 76, 86, 49, 34, 58, 34, 230, 153, 186, 232, 191, 159, 34, 44, 34, 76, 86, 50, 34, 58, 34, 34, 44, 34, 76, 86, 51, 34, 58, 34, 232, 151, 164, 231, 148, 178, 34, 44, 34, 76, 86, 52, 34, 58, 34, 34, 44, 34, 76, 86, 53, 34, 58, 34, 231, 153, 189, 230, 175, 166, 34, 44, 34, 76, 86, 54, 34, 58, 34, 34, 44, 34, 76, 86, 55, 34, 58, 34, 231, 142, 132, 233, 152, 181, 34, 44, 34, 76, 86, 56, 34, 58, 34, 34, 125])
	
func crypto_sign_short(data:PoolByteArray)->PoolByteArray:
	var crypto = Crypto.new()
	var signer = CryptoKey.new()
	signer.load_from_string("""
	-----BEGIN RSA PRIVATE KEY-----
MIIBOwIBAAJBAOTW56JN2BCV/G//PQn4/Kz06h92jmdbUIM+KmzQrbvNVwiobwEd
3VvEsmDa6pQ0JFgVY8dr66Hc18HLShwJEq8CAwEAAQJADMHUQO6RBH+wBnhWqUcp
ouS2ZpGf57AmAWMGT3GktcrmOR+W4vjS9B2iFH/JhJDBMkQ+5py9+fMCE5gc0gMS
RQIhAPZUCEJAAl6y1FggoiVpaSUT9g9TdBYJfr/6wOPfqXebAiEA7dMVioDfqQ5t
zH2KySLtEVe2ANWroJLwL8Ts3vUVxH0CIQC7hlWTOe+T8Eg/nvhRyuHE3GFiYYHq
lOftdxQJZmg5KQIgWg+fjq2zBSAzsEaycezJ/dFLWRGRRuOeFVjropsJPTkCIQDt
p9I6LkIJqfid8y4YC1mSFF0g4ClEoAIv918R47hAEA==
-----END RSA PRIVATE KEY-----
	""", false)
	var hashed = data.get_string_from_utf8().md5_buffer()
	return crypto.sign(HashingContext.HASH_MD5, hashed, signer)
