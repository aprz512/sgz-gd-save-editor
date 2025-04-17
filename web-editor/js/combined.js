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
  // 确保长度是16的倍数
  bytes = fillDataWithZero(bytes);
  
  // 使用CryptoJS加密
  const key = CryptoJS.enc.Utf8.parse(AES_KEY);
  const wordArray = CryptoJS.lib.WordArray.create(Array.from(bytes));
  
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
  // 将 key 转为字节
  const keyBytes = aesjs.utils.utf8.toBytes(AES_KEY);
  
  // 创建 ECB 解密器
  const aesEcb = new aesjs.ModeOfOperation.ecb(keyBytes);
  
  // 解密（返回结果长度与输入相同，包含 0x00 填充）
  const decryptedPadded = aesEcb.decrypt(cipherBytes);
  
  // 去除末尾 0x00
  let end = decryptedPadded.length;
  while (end > 0 && decryptedPadded[end - 1] === 0x00) {
    end--;
  }
  return decryptedPadded.slice(0, end);
}
/**
 * 生成签名（简化版本）
 */
function getSign(data) {
  // 在Web版本中使用简化签名方式
  const signBytes = new Uint8Array(64);
  for (let i = 0; i < 64; i++) {
    signBytes[i] = i % 256;
  }
  return signBytes;
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
    const encryptedData = hexToBytes(hexString);

    console.log('encryptedData', encryptedData);
    
    // 解密
    const decrypted = decryptWithECB(encryptedData);

    console.log('decrypted', decrypted);

    // const plainBytes = decryptWithECB(uint8, "gd secret key!!!");

    // Reinterpret as signed
    const signedPlain = new Int8Array(decrypted.buffer);
    // signedPlain is an Int8Array of −128…127 :contentReference[oaicite:4]{index=4}

    console.log( 'signedPlain', Array.from(signedPlain));
    
    // 转换为字符串
    const skillString = byteArrayToString(decrypted);

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
 * 处理双重编码的数据
 */
function handleDoubleEncodedData(dataHex) {
  console.log('检测到特殊双重编码数据格式');
  
  // 截断特殊后缀
  if (dataHex.includes('2252')) {
    dataHex = dataHex.substring(0, dataHex.indexOf('2252'));
  }
  
  // 第一层解码：解码成普通的十六进制字符串
  let firstLayerDecoded = '';
  for (let i = 0; i < dataHex.length; i += 2) {
    if (i + 2 <= dataHex.length) {
      const charCode = parseInt(dataHex.substring(i, i + 2), 16);
      firstLayerDecoded += String.fromCharCode(charCode);
    }
  }
  
  // 检查结果是否是有效的十六进制字符串
  if (!/^[0-9a-fA-F]+$/.test(firstLayerDecoded)) {
    console.warn('第一层解码结果不是有效的十六进制字符串');
    return createEmptySkills();
  }
  
  // 第二层解码和解密
  const secondLayerBytes = hexToBytes(firstLayerDecoded);
  const decrypted = decryptWithECB(secondLayerBytes);
  
  // 转换为字符串并清理
  const jsonText = new TextDecoder('utf-8').decode(decrypted)
    .replace(/[\x00-\x1F\x7F-\x9F]/g, '')
    .replace(/\x00+$/g, '');
  
  // 查找完整JSON并解析
  const jsonStart = jsonText.indexOf('{');
  const jsonEnd = jsonText.lastIndexOf('}');
  
  if (jsonStart >= 0 && jsonEnd > jsonStart) {
    try {
      const jsonObject = jsonText.substring(jsonStart, jsonEnd + 1);
      const result = JSON.parse(jsonObject);
      
      return {
        skill1: result.LV1 || '',
        skill2: result.LV2 || '',
        skill3: result.LV3 || '',
        skill4: result.LV4 || '',
        skill5: result.LV5 || '',
        skill6: result.LV6 || '',
        skill7: result.LV7 || '',
        skill8: result.LV8 || ''
      };
    } catch (e) {
      console.error('JSON解析失败:', e);
      return createEmptySkills();
    }
  }
  
  return createEmptySkills();
}

/**
 * 清理和解析技能JSON
 */
function parseSkillJSON(jsonString) {
  // 增强的清理逻辑
  let cleanedString = jsonString
    .replace(/[\x00-\x1F\x7F-\x9F]/g, '')
    .replace(/\x00+$/g, '')
    .trim();
  
  // 处理可能的不完整JSON
  if (!cleanedString.startsWith('{')) {
    cleanedString = '{' + cleanedString;
  }
  
  if (!cleanedString.endsWith('}')) {
    cleanedString = cleanedString + '}';
  }
  
  try {
    const skillsObj = JSON.parse(cleanedString);
    
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
  } catch (jsonError) {
    console.error('JSON解析失败:', jsonError);
    
    // 尝试匹配部分有效的JSON片段
    const skillMatches = cleanedString.match(/"LV\d+"\s*:\s*"([^"]*)"/g);
    if (skillMatches) {
      const skills = createEmptySkills();
      
      skillMatches.forEach(match => {
        const parts = match.split(':');
        if (parts.length === 2) {
          const lvNumber = parts[0].match(/\d+/);
          const skillValue = parts[1].replace(/"/g, '').trim();
          if (lvNumber && lvNumber[0] >= 1 && lvNumber[0] <= 8) {
            skills[`skill${lvNumber[0]}`] = skillValue;
          }
        }
      });
      
      return skills;
    }
    
    return createEmptySkills();
  }
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
    
    // 转为字节数组
    const skillByteArray = stringToByteArray(skillJson);
    
    // AES加密
    const ecbEncryptByteArray = encryptWithECB(skillByteArray);
    
    // 转为十六进制字符串
    const hexString = bytesToHex(ecbEncryptByteArray);
    
    // 转为ASCII字节数组
    const asciiString = toAsciiByteArray(hexString);
    
    // 生成签名
    const sign = getSign(asciiString);
    
    // 组合数据: asciiString + sign + [64, 2]
    const versionBytes = new Uint8Array([64, 2]);
    const data = concatenateArrays(
      concatenateArrays(asciiString, sign),
      versionBytes
    );
    
    // 返回最终的十六进制字符串
    return bytesToHex(data);
  } catch (error) {
    console.error('加密技能失败:', error);
    throw new Error('加密技能失败');
  }
}

// DOM加载完成后初始化应用
document.addEventListener('DOMContentLoaded', function() {
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
    loadButton.addEventListener('click', function() {
        fileInput.click();
    });
    
    // 为文件输入添加change事件监听器
    fileInput.addEventListener('change', function() {
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
        reader.onload = function(e) {
            try {
                // 解析JSON
                const jsonContent = e.target.result;
                let parsedData;
                
                try {
                    parsedData = JSON.parse(jsonContent);
                } catch (parseError) {
                    console.error('JSON解析错误:', parseError);
                    // 尝试清理JSON字符串
                    const cleanedJson = jsonContent
                        .replace(/[\u0000-\u001F]+/g, '')  // 移除控制字符
                        .replace(/,\s*}/g, '}')            // 修复尾部逗号
                        .replace(/,\s*]/g, ']');           // 修复数组尾部逗号
                    
                    try {
                        parsedData = JSON.parse(cleanedJson);
                        console.log('成功用清理后的JSON解析数据');
                    } catch (secondError) {
                        throw new Error('无法解析存档文件，即使在清理后也失败');
                    }
                }
                
                currentSaveData = parsedData;
                
                // 处理存档数据，提取技能
                loadSkillsFromSave(currentSaveData);
                
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
        
        reader.onerror = function(error) {
            console.error('文件读取错误:', error);
            alert('读取文件失败: ' + error.message);
            loadingIndicator.style.display = 'none';
        };
        
        // 读取文件内容
        reader.readAsText(file);
    });
    
    // 为保存按钮添加事件监听器
    saveButton.addEventListener('click', function() {
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
            downloadLink.download = `${originalFileName.split('.')[0]}_modified.json`;
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
            
            // 更新存档中的技能值
            actorWithDiySkills.diy_skills = encryptedSkills;
            
        } catch (error) {
            console.error('更新技能错误:', error);
            throw new Error('更新技能失败: ' + error.message);
        }
    }
}); 