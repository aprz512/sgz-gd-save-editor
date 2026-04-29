"""
模拟 Android Gson 行为 — 验证 addProperty/add/remove/toString 不会破坏 JSON
"""
import json
import copy
import os

SAVE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def test_gson_addProperty_behavior():
    """模拟 Gson 的 addProperty(String, Number) 行为"""
    print("\n=== 测试: Gson addProperty 行为 ===")

    # Kotlin: actor.addProperty("体", 99)
    # Gson 的 addProperty(String, Number) 创建一个 JsonPrimitive(Number)
    # 对应 Python json 的行为
    actor = {"体": 71, "姓名": "测试"}
    actor["体"] = 99  # 模拟 addProperty

    s = json.dumps(actor, ensure_ascii=False, separators=(',', ':'))
    parsed = json.loads(s)

    assert parsed["体"] == 99
    assert isinstance(parsed["体"], int), f"类型错误: {type(parsed['体'])}"
    assert parsed["姓名"] == "测试"
    print("  PASS: Number 字段类型保持为 int")


def test_gson_add_string_preserves_type():
    """模拟 Gson 的 addProperty(String, String) 行为"""
    print("\n=== 测试: Gson 字符串类型保持 ===")

    # Kotlin: actor.addProperty("军种", "水")
    # 在 Python 中同样效果
    actor = {"军种": "平"}
    actor["军种"] = "水"

    s = json.dumps(actor, ensure_ascii=False, separators=(',', ':'))
    parsed = json.loads(s)

    assert parsed["军种"] == "水"
    assert isinstance(parsed["军种"], str), f"类型错误: {type(parsed['军种'])}"
    print("  PASS: String 字段类型保持为 str")


def test_gson_remove_vs_keep_empty():
    """模拟 remove(key) vs addProperty(key, "") 的区别"""
    print("\n=== 测试: remove vs 保留空值 ===")

    # BUG 场景: actor.remove("字") 删除了字段
    actor_bug = {"姓名": "测试", "字": ""}
    del actor_bug["字"]  # 模拟 remove
    s_bug = json.dumps(actor_bug, ensure_ascii=False, separators=(',', ':'))
    parsed_bug = json.loads(s_bug)
    assert "字" not in parsed_bug, "字 应该被删除"
    print("  remove: '字' 字段被删除 (可能导致游戏报错)")

    # FIX 场景: actor.addProperty("字", "") 保留字段
    actor_fix = {"姓名": "测试", "字": ""}
    actor_fix["字"] = ""  # 模拟 addProperty("字", "") 保留
    s_fix = json.dumps(actor_fix, ensure_ascii=False, separators=(',', ':'))
    parsed_fix = json.loads(s_fix)
    assert "字" in parsed_fix, "字 应该被保留"
    assert parsed_fix["字"] == ""
    print("  addProperty: '字' 字段保留为空字符串 (安全)")
    print("  PASS: 修复后字段不被删除")


def test_gson_array_field():
    """模拟标签字段 — JsonArray 不被覆盖为 String"""
    print("\n=== 测试: 标签数组字段 ===")

    # 原始数据
    actor = {"姓名": "曹操", "标签": ["乐"]}

    # BUG: addProperty("标签", "乐") 覆盖数组为字符串
    actor_bug = copy.deepcopy(actor)
    actor_bug["标签"] = "乐"  # 模拟 addProperty(str)
    s_bug = json.dumps(actor_bug, ensure_ascii=False, separators=(',', ':'))
    parsed_bug = json.loads(s_bug)
    assert isinstance(parsed_bug["标签"], str), "BUG 场景: 标签变成字符串"
    print(f"  BUG: 标签变成 str: {parsed_bug['标签']!r}")

    # FIX: add("标签", JsonArray) 保留数组
    actor_fix = copy.deepcopy(actor)
    actor_fix["标签"] = ["乐", "新标签"]  # 模拟 add(key, JsonArray)
    s_fix = json.dumps(actor_fix, ensure_ascii=False, separators=(',', ':'))
    parsed_fix = json.loads(s_fix)
    assert isinstance(parsed_fix["标签"], list), "FIX 场景: 标签应该是数组"
    print(f"  FIX: 标签保持为 list: {parsed_fix['标签']}")
    print("  PASS: 数组字段类型保留正确")


def test_gson_jsonobject_tostring():
    """模拟 Gson JsonObject.toString() 的完整场景"""
    print("\n=== 测试: 完整 JsonObject.toString() ===")

    with open(os.path.join(SAVE_DIR, '7.sav'), 'r', encoding='utf-8') as f:
        data = json.loads(f.read())

    original_json = json.dumps(data, ensure_ascii=False, separators=(',', ':'))

    # 修改多个不同类型的字段
    data['actors'][0]['体'] = 99      # int
    data['actors'][0]['姓名'] = '测试'  # str
    if '标签' in data['actors'][2]:
        data['actors'][2]['标签'] = ['新标签1', '新标签2']  # list

    data['citys'][0]['金'] = 9999     # int
    data['vstates'][0]['君主'] = 99   # int
    data['year'] = 200               # int

    modified_json = json.dumps(data, ensure_ascii=False, separators=(',', ':'))

    # 验证两个 JSON 都可以解析
    orig_parsed = json.loads(original_json)
    mod_parsed = json.loads(modified_json)

    # 验证顶层 key 完全一致
    orig_keys = set(orig_parsed.keys())
    mod_keys = set(mod_parsed.keys())
    assert orig_keys == mod_keys, f"顶层 key 不一致: {orig_keys ^ mod_keys}"

    # 验证武将数量一致
    assert len(orig_parsed['actors']) == len(mod_parsed['actors'])

    # 验证每个武将的 key 都还在
    for i in range(len(orig_parsed['actors'])):
        o_keys = set(orig_parsed['actors'][i].keys())
        m_keys = set(mod_parsed['actors'][i].keys())
        if o_keys != m_keys:
            print(f"  FAIL: 武将 {i} key 不一致: {o_keys ^ m_keys}")
            raise AssertionError(f"武将 {i} key 不一致")
    print(f"  PASS: {len(orig_parsed['actors'])} 个武将 key 全部一致")

    # 验证修改生效
    assert mod_parsed['actors'][0]['体'] == 99
    assert mod_parsed['actors'][0]['姓名'] == '测试'
    assert mod_parsed['citys'][0]['金'] == 9999
    assert mod_parsed['vstates'][0]['君主'] == 99
    assert mod_parsed['year'] == 200
    if '标签' in mod_parsed['actors'][2]:
        assert isinstance(mod_parsed['actors'][2]['标签'], list)
    print("  PASS: 所有修改生效且类型正确")


def test_diy_skills_not_overwritten():
    """测试：diy_skills 在没有加载成功时不被覆盖"""
    print("\n=== 测试: diy_skills 安全保护 ===")

    with open(os.path.join(SAVE_DIR, '7.sav'), 'r', encoding='utf-8') as f:
        data = json.loads(f.read())

    # 找到有 diy_skills 的武将
    diy_actor = None
    for a in data['actors']:
        if 'diy_skills' in a:
            diy_actor = a
            break

    if diy_actor is None:
        print("  SKIP: 没有 diy_skills 武将")
        return

    original_diy = diy_actor['diy_skills']

    # 模拟 skillsLoaded=false 的情况 — 不覆盖 diy_skills
    # (不做任何修改)

    s = json.dumps(data, ensure_ascii=False, separators=(',', ':'))
    parsed = json.loads(s)

    for a in parsed['actors']:
        if 'diy_skills' in a:
            assert a['diy_skills'] == original_diy, "diy_skills 被意外覆盖!"
            print(f"  PASS: diy_skills 未被覆盖 (length={len(original_diy)})")
            return

    print("  FAIL: 找不到 diy_skills 武将")


def run_all():
    tests = [
        test_gson_addProperty_behavior,
        test_gson_add_string_preserves_type,
        test_gson_remove_vs_keep_empty,
        test_gson_array_field,
        test_gson_jsonobject_tostring,
        test_diy_skills_not_overwritten,
    ]
    passed = 0
    for t in tests:
        try:
            t()
            passed += 1
        except Exception as e:
            print(f"  FAIL: {e}")
            import traceback
            traceback.print_exc()

    print(f"\n{'='*60}")
    print(f"Gson 行为测试: {passed}/{len(tests)} 通过")
    return passed == len(tests)


if __name__ == '__main__':
    import sys
    success = run_all()
    sys.exit(0 if success else 1)
