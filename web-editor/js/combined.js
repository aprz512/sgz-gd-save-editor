/**
 * 三国志·战略版 存档编辑器
 * 合并后的JavaScript文件
 */

// AES密钥 - 所有平台通用
const AES_KEY = "gd secret key!!!";

/**
 * 将十六进制字符串解码为字节数组
 */
function hexToBytes(hexString) {
  const bytes = [];
  for (let c = 0; c < hexString.length; c += 2) {
    bytes.push(parseInt(hexString.substr(c, 2), 16));
  }
  return new Uint8Array(bytes);
}

/**
 * 将字节数组转换为十六进制字符串
 */
function bytesToHex(bytes) {
  const hexChars = [];
  for (let i = 0; i < bytes.length; i++) {
    const hex = bytes[i].toString(16).padStart(2, '0');
    hexChars.push(hex);
  }
  return hexChars.join('');
}

/**
 * 将字符串转换为ASCII字节数组
 */
function toAsciiByteArray(str) {
  const bytes = new Uint8Array(str.length);
  for (let i = 0; i < str.length; i++) {
    bytes[i] = str.charCodeAt(i);
  }
  return bytes;
}

/**
 * 字节数组转字符串
 */
function byteArrayToString(bytes) {
  let result = '';
  for (let i = 0; i < bytes.length; i++) {
    result += String.fromCharCode(bytes[i]);
  }
  return result;
}

/**
 * 字符串转字节数组（支持UTF-8编码）
 */
function stringToByteArray(str) {
  // 使用TextEncoder处理UTF-8字符，确保中文能正确编码
  const encoder = new TextEncoder();
  return encoder.encode(str);
}

/**
 * 确保数据长度是16的倍数，不足则用0填充
 */
function fillDataWithZero(bytes) {
  const blockSize = 16;
  const padLength = bytes.length % blockSize;
  if (padLength === 0) return bytes;

  const padding = new Uint8Array(blockSize - padLength);
  return concatenateArrays(bytes, padding);
}

/**
 * 合并两个Uint8Array
 */
function concatenateArrays(array1, array2) {
  const result = new Uint8Array(array1.length + array2.length);
  result.set(array1, 0);
  result.set(array2, array1.length);
  return result;
}

/**
 * 使用AES ECB模式加密
 */
function encryptWithECB(bytes) {
  // 使用CryptoJS加密
  const key = CryptoJS.enc.Utf8.parse(AES_KEY);
  const wordArray = CryptoJS.lib.WordArray.create(bytes);

  const encrypted = CryptoJS.AES.encrypt(
    wordArray,
    key,
    {
      mode: CryptoJS.mode.ECB,
      padding: CryptoJS.pad.ZeroPadding
    }
  );

  return new Uint8Array(encrypted.ciphertext.sigBytes).map((_, i) => {
    return (encrypted.ciphertext.words[i >>> 2] >>> (24 - (i % 4) * 8)) & 0xff;
  });
}

/**
 * 使用AES ECB模式解密
 */
function decryptWithECB(cipherBytes) {

  // 1. 将字节数组转换为 CryptoJS 的 WordArray 格式
  const wordArray = CryptoJS.lib.WordArray.create(cipherBytes);

  // 2. 将字节数组转换为 CryptoJS 可识别的密文格式
  const cipherParams = CryptoJS.lib.CipherParams.create({
    ciphertext: wordArray
  });

  var key1 = CryptoJS.enc.Latin1.parse(AES_KEY);
  var decrypted = CryptoJS.AES.decrypt(cipherParams, key1, {
    mode: CryptoJS.mode.ECB,
    padding: CryptoJS.pad.ZeroPadding
  });
  return decrypted.toString(CryptoJS.enc.Utf8);
}

/**
 * 使用RSA私钥对数据进行MD5withRSA签名
 * @param {Uint8Array} data - 要签名的字节数组
 * @returns {Uint8Array} - 签名结果的字节数组
 */
function getSign(data) {
  // 定义Base64编码的RSA私钥
  const privateKeyBase64 = 
    "MIIBOwIBAAJBAOTW56JN2BCV/G//PQn4/Kz06h92jmdbUIM+KmzQrbvNVwiobwEd\n" +
    "3VvEsmDa6pQ0JFgVY8dr66Hc18HLShwJEq8CAwEAAQJADMHUQO6RBH+wBnhWqUcp\n" +
    "ouS2ZpGf57AmAWMGT3GktcrmOR+W4vjS9B2iFH/JhJDBMkQ+5py9+fMCE5gc0gMS\n" +
    "RQIhAPZUCEJAAl6y1FggoiVpaSUT9g9TdBYJfr/6wOPfqXebAiEA7dMVioDfqQ5t\n" +
    "zH2KySLtEVe2ANWroJLwL8Ts3vUVxH0CIQC7hlWTOe+T8Eg/nvhRyuHE3GFiYYHq\n" +
    "lOftdxQJZmg5KQIgWg+fjq2zBSAzsEaycezJ/dFLWRGRRuOeFVjropsJPTkCIQDt\n" +
    "p9I6LkIJqfid8y4YC1mSFF0g4ClEoAIv918R47hAEA==";

  try {
    // 移除Base64字符串中的换行符并解码
    const privateKeyClean = privateKeyBase64.replace(/\n/g, '');
    const privateKeyDer = forge.util.decode64(privateKeyClean);
    
    // 从DER格式解析PKCS#8私钥
    const asn1 = forge.asn1.fromDer(privateKeyDer);
    const privateKey = forge.pki.privateKeyFromAsn1(asn1);
    
    // 创建MD5 hash对象
    const md = forge.md.md5.create();
    
    // 将Uint8Array转换为forge可处理的二进制字符串
    const dataStr = arrayBufferToString(data);
    
    // 更新哈希数据
    md.update(dataStr);
    
    // 执行签名
    const signature = privateKey.sign(md);
    
    // 将签名结果转换为Uint8Array
    return stringToUint8Array(signature);
  } catch (error) {
    console.error("签名失败:", error);
    throw error;
  }
}

/**
 * 将Uint8Array转换为forge可处理的二进制字符串
 */
function arrayBufferToString(buffer) {
  let binary = '';
  const bytes = new Uint8Array(buffer);
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return binary;
}

/**
 * 将forge二进制字符串转换为Uint8Array
 */
function stringToUint8Array(str) {
  const buf = new Uint8Array(str.length);
  for (let i = 0; i < str.length; i++) {
    buf[i] = str.charCodeAt(i);
  }
  return buf;
}

/**
 * 解密技能数据
 */
function decryptSkills(dataHex) {
  try {


    // 标准格式处理
    // 十六进制解码
    const bytesData = hexToBytes(dataHex);

    console.log(bytesData);

    // 获取签名版本和长度
    const signVersion = bytesData[bytesData.length - 1];
    const signLength = bytesData[bytesData.length - 2];

    console.log(`签名版本: ${signVersion}, 签名长度: ${signLength}`);

    // 提取技能数据部分
    const skillData = bytesData.slice(0, bytesData.length - signLength - 2);

    console.log('skillData', skillData);

    // 将 skillData 当作 ASCII 字符串（这是一个hex字符串）
    const hexString = byteArrayToString(skillData);

    console.log('hexString', hexString);

    // 将 hex 字符串转换回密文
    const hexDecode = hexToBytes(hexString);

    console.log('hexDecode', hexDecode);

    // 解密
    const skillString = decryptWithECB(hexDecode);

    console.log('skillString', skillString);

    // 清理和解析JSON
    return parseSkillJSON(skillString);

  } catch (error) {
    console.error('解密技能失败:', error);
    // 返回空技能对象而不是抛出异常
    return createEmptySkills();
  }
}

/**
 * 清理和解析技能JSON
 */
function parseSkillJSON(skillJsonString) {
  const skillsObj = JSON.parse(skillJsonString);

  return {
    skill1: skillsObj.LV1 || '',
    skill2: skillsObj.LV2 || '',
    skill3: skillsObj.LV3 || '',
    skill4: skillsObj.LV4 || '',
    skill5: skillsObj.LV5 || '',
    skill6: skillsObj.LV6 || '',
    skill7: skillsObj.LV7 || '',
    skill8: skillsObj.LV8 || ''
  };
}

/**
 * 创建空技能对象
 */
function createEmptySkills() {
  return {
    skill1: '',
    skill2: '',
    skill3: '',
    skill4: '',
    skill5: '',
    skill6: '',
    skill7: '',
    skill8: ''
  };
}

/**
 * 加密技能数据
 */
function encryptSkills(skills) {
  try {
    // 转换成后端使用的格式
    const skillsObj = {
      LV1: skills.skill1 || '',
      LV2: skills.skill2 || '',
      LV3: skills.skill3 || '',
      LV4: skills.skill4 || '',
      LV5: skills.skill5 || '',
      LV6: skills.skill6 || '',
      LV7: skills.skill7 || '',
      LV8: skills.skill8 || ''
    };

    // 转为JSON字符串
    const skillJson = JSON.stringify(skillsObj);

    console.log('skillJson', skillJson);

    // 转为字节数组
    const skillByteArray = stringToByteArray(skillJson);

    console.log('skillByteArray', skillByteArray);

    // AES加密
    const ecbEncryptByteArray = encryptWithECB(skillByteArray);

    console.log('ecbEncryptByteArray', ecbEncryptByteArray);

    // 转为十六进制字符串
    const hexString = bytesToHex(ecbEncryptByteArray);

    console.log('hexString', hexString);

    // 转为ASCII字节数组
    const asciiString = toAsciiByteArray(hexString);

    console.log('asciiString', asciiString);

    // 生成签名
    const sign = getSign(asciiString);

    console.log('sign', sign);

    // 组合数据: asciiString + sign + [64, 2]
    const versionBytes = new Uint8Array([64, 2]);

    console.log('versionBytes', versionBytes);

    const data = concatenateArrays(
      concatenateArrays(asciiString, sign),
      versionBytes
    );

    console.log('data', data);

    // 返回最终的十六进制字符串
    return bytesToHex(data);
  } catch (error) {
    console.error('加密技能失败:', error);
    throw new Error('加密技能失败');
  }
}

// DOM加载完成后初始化应用
document.addEventListener('DOMContentLoaded', function () {
  // 获取DOM元素
  const fileInput = document.getElementById('save-file');
  const loadButton = document.getElementById('load-file');
  const saveButton = document.getElementById('save-changes');
  const editorSection = document.getElementById('editor');
  const loadingIndicator = document.getElementById('loading');

  // 保存当前加载的存档数据和文件名
  let currentSaveData = null;
  let originalFileName = '';

  // 为加载按钮添加事件监听器
  loadButton.addEventListener('click', function () {
    fileInput.click();
  });

  // 为文件输入添加change事件监听器
  fileInput.addEventListener('change', function () {
    if (fileInput.files.length === 0) {
      return;
    }

    const file = fileInput.files[0];
    originalFileName = file.name;

    // 验证文件类型，接受.sav和.json文件
    const validExtensions = ['.sav', '.json'];
    const fileExt = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
    const isValidFile = validExtensions.includes(fileExt);

    if (!isValidFile) {
      alert('请上传.sav或.json格式的存档文件');
      return;
    }

    // 显示加载指示器
    loadingIndicator.style.display = 'flex';

    const reader = new FileReader();
    reader.onload = function (e) {
      try {
        // 解析JSON
        const jsonContent = e.target.result;
        let parsedData = JSON.parse(jsonContent);

        currentSaveData = parsedData;

        // 处理存档数据，提取技能
        loadSkillsFromSave(parsedData);

        // 显示编辑器区域
        editorSection.style.display = 'block';

        // 隐藏加载指示器
        loadingIndicator.style.display = 'none';
      } catch (error) {
        console.error('处理存档文件错误:', error);
        alert('解析存档文件失败: ' + error.message);
        loadingIndicator.style.display = 'none';
      }
    };

    reader.onerror = function (error) {
      console.error('文件读取错误:', error);
      alert('读取文件失败: ' + error.message);
      loadingIndicator.style.display = 'none';
    };

    // 读取文件内容
    reader.readAsText(file);
  });

  // 为保存按钮添加事件监听器
  saveButton.addEventListener('click', function () {
    if (!currentSaveData) {
      alert('请先加载存档文件');
      return;
    }

    // 显示加载指示器
    loadingIndicator.style.display = 'flex';

    try {
      // 收集用户输入的技能
      const skills = {
        skill1: document.getElementById('skill1').value.trim(),
        skill2: document.getElementById('skill2').value.trim(),
        skill3: document.getElementById('skill3').value.trim(),
        skill4: document.getElementById('skill4').value.trim(),
        skill5: document.getElementById('skill5').value.trim(),
        skill6: document.getElementById('skill6').value.trim(),
        skill7: document.getElementById('skill7').value.trim(),
        skill8: document.getElementById('skill8').value.trim()
      };

      // 更新存档中的技能
      updateSkillsInSave(currentSaveData, skills);

      // 将修改后的JSON转换为字符串
      const updatedJsonString = JSON.stringify(currentSaveData);

      // 创建并下载文件
      const blob = new Blob([updatedJsonString], { type: 'application/json' });
      const url = URL.createObjectURL(blob);

      const downloadLink = document.createElement('a');
      downloadLink.href = url;
      downloadLink.download = `${originalFileName.split('.')[0]}_modified.sav`;
      document.body.appendChild(downloadLink);
      downloadLink.click();
      document.body.removeChild(downloadLink);

      // 隐藏加载指示器
      loadingIndicator.style.display = 'none';

      alert('存档文件已成功修改并保存');
    } catch (error) {
      console.error('保存文件错误:', error);
      alert('保存文件时发生错误: ' + error.message);
      loadingIndicator.style.display = 'none';
    }
  });

  /**
   * 从存档中加载技能，并填充到编辑表单中
   */
  function loadSkillsFromSave(saveData) {
    try {
      // 查找包含diy_skills的actor
      const actors = saveData.actors;

      if (!actors || !Array.isArray(actors)) {
        console.error('存档中缺少actors数组或格式不正确');
        throw new Error('存档格式不正确，缺少actors数组');
      }

      const actorWithDiySkills = actors.find(actor => actor && 'diy_skills' in actor);

      if (!actorWithDiySkills) {
        console.warn('未找到自定义技能数据，使用空技能继续');
        fillEmptySkills();
        return;
      }

      // 获取加密的技能字符串
      const encryptedSkillsString = actorWithDiySkills.diy_skills;

      if (!encryptedSkillsString) {
        console.warn('技能字符串为空，使用空技能继续');
        fillEmptySkills();
        return;
      }

      try {
        // 解密技能
        const skills = decryptSkills(encryptedSkillsString);

        // 填充表单
        document.getElementById('skill1').value = skills.skill1 || '';
        document.getElementById('skill2').value = skills.skill2 || '';
        document.getElementById('skill3').value = skills.skill3 || '';
        document.getElementById('skill4').value = skills.skill4 || '';
        document.getElementById('skill5').value = skills.skill5 || '';
        document.getElementById('skill6').value = skills.skill6 || '';
        document.getElementById('skill7').value = skills.skill7 || '';
        document.getElementById('skill8').value = skills.skill8 || '';
      } catch (decryptError) {
        console.error('技能解密错误:', decryptError);
        alert('解密技能失败：' + decryptError.message + '。使用空技能继续。');
        fillEmptySkills();
      }
    } catch (error) {
      console.error('加载技能错误:', error);
      alert('加载技能失败: ' + error.message);
      fillEmptySkills();
    }
  }

  /**
   * 填充空技能到表单
   */
  function fillEmptySkills() {
    document.getElementById('skill1').value = '';
    document.getElementById('skill2').value = '';
    document.getElementById('skill3').value = '';
    document.getElementById('skill4').value = '';
    document.getElementById('skill5').value = '';
    document.getElementById('skill6').value = '';
    document.getElementById('skill7').value = '';
    document.getElementById('skill8').value = '';
  }

  /**
   * 使用用户输入的新技能更新存档
   */
  function updateSkillsInSave(saveData, skills) {
    try {
      // 查找包含diy_skills的actor
      const actors = saveData.actors;

      if (!actors || !Array.isArray(actors)) {
        throw new Error('存档格式不正确，缺少actors数组');
      }

      const actorWithDiySkills = actors.find(actor => actor && 'diy_skills' in actor);

      if (!actorWithDiySkills) {
        throw new Error('未找到自定义技能数据，无法更新');
      }

      // 加密技能
      const encryptedSkills = encryptSkills(skills);

      console.log('encryptedSkills', encryptedSkills);

      // 更新存档中的技能值
      actorWithDiySkills.diy_skills = encryptedSkills;

    } catch (error) {
      console.error('更新技能错误:', error);
      throw new Error('更新技能失败: ' + error.message);
    }
  }
}); 