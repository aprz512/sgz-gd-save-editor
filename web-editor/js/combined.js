/**
 * 三国志·霸王的大陆GD 存档编辑器
 */

// ============================================================
// CONSTANTS
// ============================================================
const AES_KEY = "gd secret key!!!";

const STATUS_OPTIONS = ["在野", "出仕", "死亡"];
const GENDER_OPTIONS = ["男", "女"];
const UNIT_TYPES = ["平", "山", "水", "林", "骑", "弓"];
const FACE_OPTIONS = ["", "阳", "阴"];

const ACTOR_EDIT_FIELDS = [
    { key: "ID", label: "ID", type: "readonly" },
    { key: "姓", label: "姓", type: "text" },
    { key: "姓名", label: "姓名", type: "text" },
    { key: "字", label: "字", type: "text" },
    { key: "性别", label: "性别", type: "select", options: GENDER_OPTIONS },
    { key: "体", label: "体力", type: "number", min: 0, max: 999 },
    { key: "武", label: "武力", type: "number", min: 0, max: 999 },
    { key: "知", label: "智力", type: "number", min: 0, max: 999 },
    { key: "政", label: "政治", type: "number", min: 0, max: 999 },
    { key: "统", label: "统率", type: "number", min: 0, max: 999 },
    { key: "德", label: "德行", type: "number", min: 0, max: 999 },
    { key: "忠", label: "忠诚", type: "number", min: 0, max: 999 },
    { key: "胆", label: "胆力", type: "number", min: 0, max: 999 },
    { key: "等级", label: "等级", type: "number", min: 0, max: 99 },
    { key: "经验", label: "经验", type: "number", min: 0, max: 9999 },
    { key: "兵力", label: "兵力", type: "number", min: 0, max: 99999 },
    { key: "军种", label: "军种", type: "select", options: UNIT_TYPES },
    { key: "状态", label: "状态", type: "select", options: STATUS_OPTIONS },
    { key: "所在城", label: "所在城", type: "number", min: 0, max: 42 },
    { key: "流放地", label: "流放地", type: "number", min: 0, max: 42 },
    { key: "坐骑", label: "坐骑ID", type: "number", min: 0, max: 99 },
    { key: "武器", label: "武器ID", type: "number", min: 0, max: 99 },
    { key: "防具", label: "防具ID", type: "number", min: 0, max: 99 },
    { key: "道具", label: "道具ID", type: "number", min: 0, max: 99 },
    { key: "大限", label: "大限", type: "number", min: 0, max: 999 },
    { key: "相性", label: "相性", type: "number", min: 0, max: 99 },
    { key: "面", label: "阴阳面", type: "select", options: FACE_OPTIONS },
    { key: "标签", label: "标签", type: "text" },
];

const CITY_EDIT_FIELDS = [
    { key: "ID", label: "ID", type: "readonly" },
    { key: "城池名", label: "城池名", type: "text" },
    { key: "行政区", label: "行政区", type: "text" },
    { key: "地域", label: "地域", type: "text" },
    { key: "归属", label: "归属势力ID", type: "number", min: -1, max: 20 },
    { key: "金", label: "金", type: "number", min: 0, max: 99999 },
    { key: "米", label: "米", type: "number", min: 0, max: 999999 },
    { key: "人口", label: "人口", type: "number", min: 0, max: 9999999 },
    { key: "土地", label: "土地", type: "number", min: 0, max: 999 },
    { key: "产业", label: "产业", type: "number", min: 0, max: 999 },
    { key: "统治度", label: "统治度", type: "number", min: 0, max: 100 },
    { key: "后备兵", label: "后备兵", type: "number", min: 0, max: 99999 },
    { key: "防灾", label: "防灾", type: "number", min: 0, max: 100 },
    { key: "买米价", label: "买米价", type: "number", min: 0, max: 100 },
    { key: "卖米价", label: "卖米价", type: "number", min: 0, max: 100 },
    { key: "旱灾", label: "旱灾", type: "select", options: ["false", "true"] },
    { key: "洪涝", label: "洪涝", type: "select", options: ["false", "true"] },
];

const FACTION_EDIT_FIELDS = [
    { key: "ID", label: "ID", type: "readonly" },
    { key: "君主", label: "君主ID", type: "number", min: 0, max: 999 },
    { key: "目标城池", label: "目标城池ID", type: "number", min: 0, max: 42 },
    { key: "状态", label: "状态", type: "text" },
];


// ============================================================
// CRYPTO (unchanged from original)
// ============================================================
function hexToBytes(hexString) {
    const bytes = [];
    for (let c = 0; c < hexString.length; c += 2) {
        bytes.push(parseInt(hexString.substr(c, 2), 16));
    }
    return new Uint8Array(bytes);
}

function bytesToHex(bytes) {
    const hexChars = [];
    for (let i = 0; i < bytes.length; i++) {
        hexChars.push(bytes[i].toString(16).padStart(2, '0'));
    }
    return hexChars.join('');
}

function toAsciiByteArray(str) {
    return new Uint8Array([...str].map(c => c.charCodeAt(0) & 0x7f));
}

function byteArrayToString(bytes) {
    return String.fromCharCode(...bytes);
}

function stringToByteArray(str) {
    return new TextEncoder().encode(str);
}

function concatenateArrays(a, b) {
    const r = new Uint8Array(a.length + b.length);
    r.set(a, 0);
    r.set(b, a.length);
    return r;
}

function encryptWithECB(bytes) {
    const key = CryptoJS.enc.Utf8.parse(AES_KEY);
    const wordArray = CryptoJS.lib.WordArray.create(bytes);
    const encrypted = CryptoJS.AES.encrypt(wordArray, key, {
        mode: CryptoJS.mode.ECB,
        padding: CryptoJS.pad.ZeroPadding
    });
    return new Uint8Array(encrypted.ciphertext.sigBytes).map((_, i) =>
        (encrypted.ciphertext.words[i >>> 2] >>> (24 - (i % 4) * 8)) & 0xff
    );
}

function decryptWithECB(cipherBytes) {
    const wordArray = CryptoJS.lib.WordArray.create(cipherBytes);
    const cipherParams = CryptoJS.lib.CipherParams.create({ ciphertext: wordArray });
    const key1 = CryptoJS.enc.Latin1.parse(AES_KEY);
    return CryptoJS.AES.decrypt(cipherParams, key1, {
        mode: CryptoJS.mode.ECB,
        padding: CryptoJS.pad.ZeroPadding
    }).toString(CryptoJS.enc.Utf8);
}

function getSign(data) {
    const privateKeyBase64 =
        "MIIBOwIBAAJBAOTW56JN2BCV/G//PQn4/Kz06h92jmdbUIM+KmzQrbvNVwiobwEd\n" +
        "3VvEsmDa6pQ0JFgVY8dr66Hc18HLShwJEq8CAwEAAQJADMHUQO6RBH+wBnhWqUcp\n" +
        "ouS2ZpGf57AmAWMGT3GktcrmOR+W4vjS9B2iFH/JhJDBMkQ+5py9+fMCE5gc0gMS\n" +
        "RQIhAPZUCEJAAl6y1FggoiVpaSUT9g9TdBYJfr/6wOPfqXebAiEA7dMVioDfqQ5t\n" +
        "zH2KySLtEVe2ANWroJLwL8Ts3vUVxH0CIQC7hlWTOe+T8Eg/nvhRyuHE3GFiYYHq\n" +
        "lOftdxQJZmg5KQIgWg+fjq2zBSAzsEaycezJ/dFLWRGRRuOeFVjropsJPTkCIQDt\n" +
        "p9I6LkIJqfid8y4YC1mSFF0g4ClEoAIv918R47hAEA==";
    const privateKeyClean = privateKeyBase64.replace(/\n/g, '');
    const privateKeyDer = forge.util.decode64(privateKeyClean);
    const asn1 = forge.asn1.fromDer(privateKeyDer);
    const privateKey = forge.pki.privateKeyFromAsn1(asn1);
    const md = forge.md.md5.create();
    const dataStr = String.fromCharCode(...data);
    md.update(dataStr);
    const signature = privateKey.sign(md);
    return new Uint8Array([...signature].map(c => c.charCodeAt(0)));
}

function decryptSkills(dataHex) {
    try {
        const bytesData = hexToBytes(dataHex);
        const signVersion = bytesData[bytesData.length - 1];
        const signLength = bytesData[bytesData.length - 2];
        const skillData = bytesData.slice(0, bytesData.length - signLength - 2);
        const hexString = byteArrayToString(skillData);
        const hexDecode = hexToBytes(hexString);
        const skillString = decryptWithECB(hexDecode);
        const skillsObj = JSON.parse(skillString);
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
    } catch (e) {
        console.error('decryptSkills failed:', e);
        return { skill1: '', skill2: '', skill3: '', skill4: '', skill5: '', skill6: '', skill7: '', skill8: '' };
    }
}

function encryptSkills(skills) {
    const skillsObj = {
        LV1: skills.skill1 || '', LV2: skills.skill2 || '',
        LV3: skills.skill3 || '', LV4: skills.skill4 || '',
        LV5: skills.skill5 || '', LV6: skills.skill6 || '',
        LV7: skills.skill7 || '', LV8: skills.skill8 || ''
    };
    const skillJson = JSON.stringify(skillsObj);
    const skillByteArray = stringToByteArray(skillJson);
    const ecbEncryptByteArray = encryptWithECB(skillByteArray);
    const hexString = bytesToHex(ecbEncryptByteArray);
    const asciiString = toAsciiByteArray(hexString);
    const sign = getSign(asciiString);
    const versionBytes = new Uint8Array([64, 2]);
    const data = concatenateArrays(concatenateArrays(asciiString, sign), versionBytes);
    return bytesToHex(data);
}

// ============================================================
// STATE
// ============================================================
let currentSaveData = null;
let originalFileName = '';
let editingActor = null;
let editingCity = null;
let editingFaction = null;
let cityMap = {};
let factionMap = {};
let actorNameMap = {};

// ============================================================
// DOM READY
// ============================================================
document.addEventListener('DOMContentLoaded', () => {
    const fileInput = document.getElementById('save-file');
    const loadButton = document.getElementById('load-file');
    const saveButton = document.getElementById('save-changes');
    const editorDiv = document.getElementById('editor');
    const uploadSection = document.getElementById('upload-section');
    const loadingIndicator = document.getElementById('loading');

    // File load
    loadButton.addEventListener('click', () => fileInput.click());

    fileInput.addEventListener('change', () => {
        if (!fileInput.files.length) return;
        const file = fileInput.files[0];
        originalFileName = file.name;

        const validExts = ['.sav', '.json'];
        const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
        if (!validExts.includes(ext)) {
            showToast('请上传 .sav 或 .json 格式的存档文件');
            return;
        }

        showLoading();
        const reader = new FileReader();
        reader.onload = (e) => {
            try {
                currentSaveData = JSON.parse(e.target.result);
                buildMaps();
                renderAll();
                uploadSection.style.display = 'none';
                editorDiv.style.display = 'block';
            } catch (err) {
                showToast('解析存档文件失败: ' + err.message);
            }
            hideLoading();
        };
        reader.onerror = () => {
            showToast('读取文件失败');
            hideLoading();
        };
        reader.readAsText(file);
    });

    // Save
    saveButton.addEventListener('click', () => {
        if (!currentSaveData) return;
        try {
            saveSkillsFromForm();
            const json = JSON.stringify(currentSaveData);
            const blob = new Blob([json], { type: 'application/json' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = originalFileName.replace(/\.\w+$/, '') + '_modified.sav';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
            showToast('存档已保存并下载');
        } catch (err) {
            showToast('保存失败: ' + err.message);
        }
    });

    // Tab switching
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
            btn.classList.add('active');
            document.getElementById('tab-' + btn.dataset.tab).classList.add('active');
        });
    });

    // Equipment sub-tabs
    document.querySelectorAll('.equip-tab').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.equip-tab').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            renderEquipTable(btn.dataset.equip);
        });
    });

    // Actor search/filter
    document.getElementById('actor-search').addEventListener('input', renderActorTable);
    document.getElementById('actor-status-filter').addEventListener('change', renderActorTable);
    document.getElementById('actor-faction-filter').addEventListener('change', renderActorTable);

    // Modal
    document.getElementById('modal-close').addEventListener('click', closeModal);
    document.getElementById('modal-cancel').addEventListener('click', closeModal);
    document.getElementById('modal-save').addEventListener('click', saveModalActor);
    document.getElementById('actor-modal').addEventListener('click', (e) => {
        if (e.target === e.currentTarget) closeModal();
    });
});

// ============================================================
// HELPERS
// ============================================================
function showLoading() { document.getElementById('loading').style.display = 'flex'; }
function hideLoading() { document.getElementById('loading').style.display = 'none'; }

function showToast(msg) {
    const existing = document.querySelector('.toast');
    if (existing) existing.remove();
    const el = document.createElement('div');
    el.className = 'toast';
    el.textContent = msg;
    document.body.appendChild(el);
    setTimeout(() => el.remove(), 2500);
}

function buildMaps() {
    cityMap = {};
    factionMap = {};
    actorNameMap = {};
    if (currentSaveData.citys) {
        currentSaveData.citys.forEach(c => { cityMap[c.ID] = c; });
    }
    if (currentSaveData.vstates) {
        currentSaveData.vstates.forEach(v => { factionMap[v.ID] = v; });
    }
    if (currentSaveData.actors) {
        currentSaveData.actors.forEach(a => { actorNameMap[a.ID] = a; });
    }
    // Populate faction filter
    const sel = document.getElementById('actor-faction-filter');
    sel.innerHTML = '<option value="">全部势力</option>';
    if (currentSaveData.vstates) {
        currentSaveData.vstates.forEach(v => {
            const monarch = actorNameMap[v.君主];
            const name = monarch ? monarch.姓名 : ('势力' + v.ID);
            sel.innerHTML += `<option value="${v.ID}">${name}</option>`;
        });
    }
}

function getCityName(cityId) {
    const c = cityMap[cityId];
    return c ? c.城池名 : (cityId === undefined || cityId === 0 ? '' : cityId);
}

function getFactionOfActor(actor) {
    if (!actor.所在城 && actor.所在城 !== 0) return null;
    const city = cityMap[actor.所在城];
    if (!city || city.归属 < 0) return null;
    return factionMap[city.归属] || null;
}

function getActorName(id) {
    const a = actorNameMap[id];
    return a ? a.姓名 : (id !== undefined ? 'ID:' + id : '');
}

function getFactionName(faction) {
    if (!faction) return '';
    const monarch = actorNameMap[faction.君主];
    return monarch ? monarch.姓名 : ('势力' + faction.ID);
}

// ============================================================
// RENDER ALL
// ============================================================
function renderAll() {
    buildMaps();
    renderActorTable();
    loadSkillsToForm();
    renderCityTable();
    renderFactionTable();
    renderSettings();
    renderEquipTable('weapons');
}

// ============================================================
// ACTOR TABLE
// ============================================================
function renderActorTable() {
    if (!currentSaveData || !currentSaveData.actors) return;
    const search = (document.getElementById('actor-search').value || '').toLowerCase();
    const statusFilter = document.getElementById('actor-status-filter').value;
    const factionFilter = document.getElementById('actor-faction-filter').value;

    let actors = currentSaveData.actors.slice();
    if (statusFilter) actors = actors.filter(a => a.状态 === statusFilter);
    if (factionFilter) {
        actors = actors.filter(a => {
            const f = getFactionOfActor(a);
            return f && String(f.ID) === factionFilter;
        });
    }
    if (search) {
        actors = actors.filter(a =>
            (a.姓名 && a.姓名.toLowerCase().includes(search)) ||
            (a.字 && a.字.toLowerCase().includes(search)) ||
            (a.姓 && a.姓.toLowerCase().includes(search))
        );
    }

    document.getElementById('actor-count').textContent = actors.length + '/' + currentSaveData.actors.length;
    const tbody = document.getElementById('actor-tbody');
    tbody.innerHTML = actors.map(a => {
        const cityName = getCityName(a.所在城);
        const statusClass = a.状态 || '';
        return `<tr data-actor-id="${a.ID}" class="actor-row">
            <td>${a.ID}</td>
            <td class="editable" data-field="姓名">${esc(a.姓名)}</td>
            <td class="editable" data-field="字">${esc(a.字)}</td>
            <td>${esc(a.性别)}</td>
            <td class="number editable" data-field="体">${a.体}</td>
            <td class="number editable" data-field="武">${a.武}</td>
            <td class="number editable" data-field="知">${a.知}</td>
            <td class="number editable" data-field="政">${a.政}</td>
            <td class="number editable" data-field="统">${a.统}</td>
            <td class="number editable" data-field="德">${a.德}</td>
            <td class="number editable" data-field="忠">${a.忠}</td>
            <td class="number editable" data-field="胆">${a.胆}</td>
            <td class="number editable" data-field="等级">${a.等级}</td>
            <td class="number editable" data-field="经验">${a.经验}</td>
            <td class="number editable" data-field="兵力">${a.兵力}</td>
            <td class="editable" data-field="军种">${esc(a.军种)}</td>
            <td><span class="badge badge-${statusClass}">${esc(a.状态)}</span></td>
            <td>${cityName}</td>
            <td class="number">${a.武器 ?? ''}</td>
            <td class="number">${a.防具 ?? ''}</td>
            <td class="number">${a.道具 ?? ''}</td>
            <td class="number">${a.坐骑 ?? ''}</td>
        </tr>`;
    }).join('');

    // Click handlers - editable cells open inline editor, row click opens modal
    tbody.querySelectorAll('tr').forEach(row => {
        row.addEventListener('click', () => openActorModal(parseInt(row.dataset.actorId)));
    });
}

function esc(s) { return s == null ? '' : String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }

// ============================================================
// MODALS
// ============================================================

function buildModalForm(fields, obj) {
    return fields.map(f => {
        const val = obj[f.key] != null ? String(obj[f.key]) : '';
        let input;
        if (f.type === 'readonly') {
            input = `<input type="text" value="${esc(val)}" readonly data-key="${f.key}">`;
        } else if (f.type === 'select') {
            input = `<select data-key="${f.key}">${f.options.map(o => `<option value="${esc(o)}" ${o === val ? 'selected' : ''}>${esc(o || '(空)')}</option>`).join('')}</select>`;
        } else if (f.type === 'number') {
            input = `<input type="number" value="${esc(val)}" data-key="${f.key}" min="${f.min ?? ''}" max="${f.max ?? ''}">`;
        } else {
            input = `<input type="text" value="${esc(val)}" data-key="${f.key}">`;
        }
        return `<div class="form-row"><label>${f.label}</label>${input}</div>`;
    }).join('');
}

function saveFromModalForm(fields, obj) {
    const body = document.getElementById('modal-body');
    const inputs = body.querySelectorAll('input, select');
    inputs.forEach(inp => {
        const key = inp.dataset.key;
        const field = fields.find(f => f.key === key);
        if (!field || field.type === 'readonly') return;
        let val = inp.value.trim();
        if (field.type === 'number') {
            if (val === '') { delete obj[key]; return; }
            val = Number(val);
        }
        obj[key] = val;
    });
    // Handle 标签 as array
    if (obj.标签 && typeof obj.标签 === 'string') {
        obj.标签 = obj.标签.split(/[,，]/).map(s => s.trim()).filter(Boolean);
    }
    // Handle boolean fields
    for (const f of fields) {
        if (f.type === 'select' && f.options && f.options[0] === 'false') {
            obj[f.key] = obj[f.key] === 'true';
        }
    }
}

function openActorModal(actorId) {
    const actor = currentSaveData.actors.find(a => a.ID === actorId);
    if (!actor) return;
    editingActor = actor;
    editingCity = null;
    editingFaction = null;
    document.getElementById('modal-title').textContent = `编辑武将 — ${actor.姓名} (ID: ${actor.ID})`;
    document.getElementById('modal-body').innerHTML = buildModalForm(ACTOR_EDIT_FIELDS, actor);
    document.getElementById('actor-modal').style.display = 'flex';
}

function openCityModal(cityId) {
    const city = currentSaveData.citys.find(c => c.ID === cityId);
    if (!city) return;
    editingActor = null;
    editingCity = city;
    editingFaction = null;
    // Convert booleans to string for select
    const cityCopy = Object.assign({}, city);
    if (typeof cityCopy.旱灾 === 'boolean') cityCopy.旱灾 = String(cityCopy.旱灾);
    if (typeof cityCopy.洪涝 === 'boolean') cityCopy.洪涝 = String(cityCopy.洪涝);
    document.getElementById('modal-title').textContent = `编辑城池 — ${city.城池名} (ID: ${city.ID})`;
    document.getElementById('modal-body').innerHTML = buildModalForm(CITY_EDIT_FIELDS, cityCopy);
    document.getElementById('actor-modal').style.display = 'flex';
}

function openFactionModal(factionId) {
    const faction = currentSaveData.vstates.find(v => v.ID === factionId);
    if (!faction) return;
    editingActor = null;
    editingCity = null;
    editingFaction = faction;
    document.getElementById('modal-title').textContent = `编辑势力 — ${getFactionName(faction)} (ID: ${faction.ID})`;
    document.getElementById('modal-body').innerHTML = buildModalForm(FACTION_EDIT_FIELDS, faction);
    // Add extra info rows
    const extra = document.createElement('div');
    extra.className = 'form-row full';
    extra.innerHTML = `<label>友好势力</label><input type="text" value="${esc((faction.友好势力 || []).join(','))}" data-key="友好势力_raw">`;
    document.getElementById('modal-body').appendChild(extra);
    const extra2 = document.createElement('div');
    extra2.className = 'form-row full';
    extra2.innerHTML = `<label>仇恨势力</label><input type="text" value="${esc((faction.仇恨势力 || []).join(','))}" data-key="仇恨势力_raw">`;
    document.getElementById('modal-body').appendChild(extra2);
    const extra3 = document.createElement('div');
    extra3.className = 'form-row full';
    extra3.innerHTML = `<label>继承人</label><input type="text" value="${esc((faction.继承人 || []).join(','))}" data-key="继承人_raw">`;
    document.getElementById('modal-body').appendChild(extra3);
    document.getElementById('actor-modal').style.display = 'flex';
}

function closeModal() {
    document.getElementById('actor-modal').style.display = 'none';
    editingActor = null;
    editingCity = null;
    editingFaction = null;
}

function saveModalActor() {
    if (editingActor) {
        saveFromModalForm(ACTOR_EDIT_FIELDS, editingActor);
        closeModal();
        renderActorTable();
        showToast('武将数据已更新');
    } else if (editingCity) {
        saveFromModalForm(CITY_EDIT_FIELDS, editingCity);
        closeModal();
        renderCityTable();
        showToast('城池数据已更新');
    } else if (editingFaction) {
        saveFromModalForm(FACTION_EDIT_FIELDS, editingFaction);
        // Handle extra faction fields
        const body = document.getElementById('modal-body');
        const rawFriends = body.querySelector('[data-key="友好势力_raw"]');
        const rawEnemies = body.querySelector('[data-key="仇恨势力_raw"]');
        const rawHeirs = body.querySelector('[data-key="继承人_raw"]');
        if (rawFriends) editingFaction.友好势力 = rawFriends.value.split(/[,，]/).map(s => Number(s.trim())).filter(n => !isNaN(n));
        if (rawEnemies) editingFaction.仇恨势力 = rawEnemies.value.split(/[,，]/).map(s => Number(s.trim())).filter(n => !isNaN(n));
        if (rawHeirs) editingFaction.继承人 = rawHeirs.value.split(/[,，]/).map(s => Number(s.trim())).filter(n => !isNaN(n));
        closeModal();
        renderFactionTable();
        showToast('势力数据已更新');
    }
}

// ============================================================
// SKILLS
// ============================================================
function loadSkillsToForm() {
    try {
        const actors = currentSaveData.actors;
        if (!actors) return;
        const actor = actors.find(a => a && 'diy_skills' in a);
        if (!actor || !actor.diy_skills) {
            for (let i = 1; i <= 8; i++) document.getElementById('skill' + i).value = '';
            return;
        }
        const skills = decryptSkills(actor.diy_skills);
        for (let i = 1; i <= 8; i++) {
            document.getElementById('skill' + i).value = skills['skill' + i] || '';
        }
    } catch (e) {
        console.error('loadSkillsToForm:', e);
    }
}

function saveSkillsFromForm() {
    try {
        const actors = currentSaveData.actors;
        if (!actors) return;
        const actor = actors.find(a => a && 'diy_skills' in a);
        if (!actor) return;
        const skills = {};
        for (let i = 1; i <= 8; i++) {
            skills['skill' + i] = document.getElementById('skill' + i).value.trim();
        }
        actor.diy_skills = encryptSkills(skills);
    } catch (e) {
        console.error('saveSkillsFromForm:', e);
    }
}

// ============================================================
// CITY TABLE
// ============================================================
function renderCityTable() {
    if (!currentSaveData || !currentSaveData.citys) return;
    const cities = currentSaveData.citys;
    document.getElementById('city-count').textContent = cities.length;

    const tbody = document.getElementById('city-tbody');
    tbody.innerHTML = cities.map(c => `<tr data-city-id="${c.ID}">
        <td>${c.ID}</td>
        <td>${esc(c.城池名)}</td>
        <td>${esc(c.行政区)}</td>
        <td>${esc(c.地域)}</td>
        <td>${c.归属 >= 0 ? getFactionName(factionMap[c.归属]) : '无'}</td>
        <td class="number">${c.金}</td>
        <td class="number">${c.米}</td>
        <td class="number">${c.人口}</td>
        <td class="number">${c.土地}</td>
        <td class="number">${c.产业}</td>
        <td class="number">${c.统治度}</td>
        <td class="number">${c.后备兵}</td>
        <td class="number">${c.防灾}</td>
        <td class="number">${c.买米价}</td>
        <td class="number">${c.卖米价}</td>
        <td>${c.旱灾 ? '是' : '否'}</td>
        <td>${c.洪涝 ? '是' : '否'}</td>
    </tr>`).join('');

    tbody.querySelectorAll('tr').forEach(row => {
        row.addEventListener('click', () => openCityModal(parseInt(row.dataset.cityId)));
    });
}

// ============================================================
// FACTION TABLE
// ============================================================
function renderFactionTable() {
    if (!currentSaveData || !currentSaveData.vstates) return;
    const factions = currentSaveData.vstates;
    document.getElementById('faction-count').textContent = factions.length;

    const tbody = document.getElementById('faction-tbody');
    tbody.innerHTML = factions.map(v => {
        const monarchName = getActorName(v.君主);
        const targetCity = getCityName(v.目标城池);
        const status = v.状态 || '正常';
        const friends = (v.友好势力 || []).map(id => getFactionName(factionMap[id])).filter(Boolean).join(', ');
        const enemies = (v.仇恨势力 || []).map(id => getFactionName(factionMap[id])).filter(Boolean).join(', ');
        const heirs = (v.继承人 || []).map(id => getActorName(id)).filter(Boolean).join(', ');
        return `<tr data-faction-id="${v.ID}">
            <td>${v.ID}</td>
            <td>${esc(monarchName)}</td>
            <td>${esc(targetCity)}</td>
            <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis" title="${esc(heirs)}">${esc(heirs)}</td>
            <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis" title="${esc(friends)}">${esc(friends) || '无'}</td>
            <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis" title="${esc(enemies)}">${esc(enemies) || '无'}</td>
            <td>${esc(status)}</td>
        </tr>`;
    }).join('');

    tbody.querySelectorAll('tr').forEach(row => {
        row.addEventListener('click', () => openFactionModal(parseInt(row.dataset.factionId)));
    });
}

// ============================================================
// GAME SETTINGS
// ============================================================
function renderSettings() {
    if (!currentSaveData) return;
    document.getElementById('set-year').value = currentSaveData.year || 189;
    document.getElementById('set-month').value = currentSaveData.month || 1;
    document.getElementById('set-savetime').value = currentSaveData.存档时间 || '';
    if (currentSaveData.players && currentSaveData.players[0]) {
        document.getElementById('set-power').value = currentSaveData.players[0].power_value || 50;
    }

    // Game set fields
    const gs = currentSaveData.game_set || {};
    const container = document.getElementById('game-set-container');
    const fields = Object.keys(gs);
    if (fields.length === 0) {
        container.innerHTML = '<p style="color:var(--text-muted)">无设置项</p>';
        return;
    }
    container.innerHTML = fields.map(k => {
        const val = gs[k];
        return `<div class="form-row">
            <label>${esc(k)}</label>
            <input type="text" data-gameset-key="${esc(k)}" value="${esc(String(val))}">
        </div>`;
    }).join('');

    // Bind change handlers
    document.querySelectorAll('[data-gameset-key]').forEach(inp => {
        inp.addEventListener('change', () => {
            currentSaveData.game_set[inp.dataset.gamesetKey] = inp.value;
        });
    });

    document.getElementById('set-year').addEventListener('change', function() { currentSaveData.year = parseInt(this.value) || 189; });
    document.getElementById('set-month').addEventListener('change', function() { currentSaveData.month = parseInt(this.value) || 1; });
    document.getElementById('set-power').addEventListener('change', function() {
        if (currentSaveData.players && currentSaveData.players[0]) {
            currentSaveData.players[0].power_value = parseInt(this.value) || 50;
        }
    });
}

// ============================================================
// EQUIPMENT REFERENCE
// ============================================================
function renderEquipTable(type) {
    const container = document.getElementById('equip-table-container');
    const data = currentSaveData[type];
    if (!data || !data.length) {
        container.innerHTML = '<p style="color:var(--text-muted);padding:20px">无数据</p>';
        return;
    }

    // 检测格式：新存档只有 ID+数量, 旧存档有详细属性
    const first = data[0];
    const isNewFormat = Object.keys(first).length <= 2 && ('数量' in first);

    let columns, rows;
    if (isNewFormat) {
        columns = ['ID', '数量'];
        rows = data.map(w => [w.ID, w.数量]);
    } else if (type === 'weapons') {
        columns = ['ID', '名称', '品质', '类型', '持法', '攻击力', '防御力', '重量', '价格', '描述'];
        rows = data.map(w => [
            w.ID, w.名称, w.品质, w.类型, w.持法 || '', w.攻击力 ?? '', w.防御力 ?? '', w.重量 ?? '',
            w.价格 ?? '', (w.描述 || '').replace(/①/g, '')
        ]);
    } else if (type === 'steeds') {
        columns = ['ID', '名称', '品质', '马力', '机动力恢复', '价格', '购买条件', '描述'];
        rows = data.map(s => [
            s.ID, s.名称, s.品质, s.马力 ?? '', s.机动力恢复 ?? '', s.价格 ?? '',
            s.购买条件 || '', (s.描述 || '').replace(/①/g, '')
        ]);
    } else if (type === 'suits') {
        columns = ['ID', '名称', '品质', '防御力', '攻击力', '重量', '价格', '购买条件', '描述'];
        rows = data.map(s => [
            s.ID, s.名称, s.品质, s.防御力 ?? '', s.攻击力 ?? '', s.重量 ?? '', s.价格 ?? '',
            s.购买条件 || '', (s.描述 || '').replace(/①/g, '')
        ]);
    } else {
        columns = ['ID', '名称', '品质', '类型', '射程', '价格', '购买条件', '描述'];
        rows = data.map(j => [
            j.ID, j.名称, j.品质, j.类型 || '', j.射程 ?? '', j.价格 ?? '',
            j.购买条件 || '', (j.描述 || '').replace(/①/g, '')
        ]);
    }

    container.innerHTML = `<table class="data-table">
        <thead><tr>${columns.map(c => `<th>${c}</th>`).join('')}</tr></thead>
        <tbody>${rows.map(r => `<tr>${r.map(c => `<td>${esc(String(c ?? ''))}</td>`).join('')}</tr>`).join('')}</tbody>
    </table>`;
}
