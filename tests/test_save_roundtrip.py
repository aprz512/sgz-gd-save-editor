"""
测试存档修改的完整 round-trip：
1. 加载存档 → 2. 修改数据 → 3. 序列化 → 4. 重新解析验证
"""
import json
import copy
import sys
import os

SAVE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

def load_save(filename):
    path = os.path.join(SAVE_DIR, filename)
    with open(path, 'r', encoding='utf-8') as f:
        raw = f.read()
    return json.loads(raw), raw

def serialize(data):
    """模拟 Gson JsonObject.toString() — 紧凑 JSON"""
    return json.dumps(data, ensure_ascii=False, separators=(',', ':'))

def deep_compare_keys(d1, d2, path="root"):
    """递归比较两个 JSON 的所有 key，返回差异列表"""
    diffs = []
    if isinstance(d1, dict) and isinstance(d2, dict):
        k1, k2 = set(d1.keys()), set(d2.keys())
        only_in_1 = k1 - k2
        only_in_2 = k2 - k1
        if only_in_1:
            diffs.append(f"{path}: 缺失键 {only_in_1}")
        if only_in_2:
            diffs.append(f"{path}: 多余键 {only_in_2}")
        for k in k1 & k2:
            diffs.extend(deep_compare_keys(d1[k], d2[k], f"{path}.{k}"))
    elif isinstance(d1, list) and isinstance(d2, list):
        for i in range(min(len(d1), len(d2))):
            diffs.extend(deep_compare_keys(d1[i], d2[i], f"{path}[{i}]"))
    return diffs


def test_actor_modify_structure(filename):
    """测试：修改武将属性后 JSON 结构不变"""
    print(f"\n{'='*60}")
    print(f"测试: 修改武将属性 [{filename}]")
    print(f"{'='*60}")

    data, _ = load_save(filename)
    original = copy.deepcopy(data)

    # 修改第一个武将的多个字段
    actor = data['actors'][0]
    actor['体'] = 99       # 数字
    actor['武'] = 88
    actor['姓名'] = '测试'  # 字符串

    # 如果有标签，修改标签
    if '标签' in actor:
        actor['标签'] = ['新标签']

    # 序列化 & 重新解析
    modified_str = serialize(data)
    try:
        modified = json.loads(modified_str)
    except json.JSONDecodeError as e:
        print(f"  FAIL: 修改后的 JSON 无效: {e}")
        return False

    # 比较结构
    diffs = deep_compare_keys(original, modified)
    # 过滤掉我们故意修改的字段
    real_diffs = [d for d in diffs if '体' not in d and '武' not in d and '姓名' not in d.split('.')[-1] and '标签' not in d.split('.')[-1]]

    if real_diffs:
        print(f"  FAIL: 意外的结构变化:")
        for d in real_diffs:
            print(f"    {d}")
        return False

    # 验证修改生效
    assert modified['actors'][0]['体'] == 99
    assert modified['actors'][0]['武'] == 88
    assert modified['actors'][0]['姓名'] == '测试'

    print(f"  PASS: 武将修改正常, {len(data['actors'])} 个武将结构完整")
    return True


def test_city_modify_structure(filename):
    """测试：修改城池属性后 JSON 结构不变"""
    print(f"\n{'='*60}")
    print(f"测试: 修改城池属性 [{filename}]")
    print(f"{'='*60}")

    data, _ = load_save(filename)
    original = copy.deepcopy(data)

    # 修改第一个城池
    city = data['citys'][0]
    city['金'] = 9999
    city['米'] = 8888
    city['人口'] = 50000
    city['统治度'] = 100

    modified_str = serialize(data)
    modified = json.loads(modified_str)

    diffs = deep_compare_keys(original, modified)
    real_diffs = [d for d in diffs
                  if '金' not in d.split('.')[-1]
                  and '米' not in d.split('.')[-1]
                  and '人口' not in d.split('.')[-1]
                  and '统治度' not in d.split('.')[-1]]

    if real_diffs:
        print(f"  FAIL: 意外的结构变化:")
        for d in real_diffs:
            print(f"    {d}")
        return False

    print(f"  PASS: 城池修改正常, {len(data['citys'])} 个城池结构完整")
    return True


def test_faction_modify_structure(filename):
    """测试：修改势力属性后 JSON 结构不变"""
    print(f"\n{'='*60}")
    print(f"测试: 修改势力属性 [{filename}]")
    print(f"{'='*60}")

    data, _ = load_save(filename)
    original = copy.deepcopy(data)

    faction = data['vstates'][0]
    faction['君主'] = 99
    faction['目标城池'] = 5

    modified_str = serialize(data)
    modified = json.loads(modified_str)

    diffs = deep_compare_keys(original, modified)
    real_diffs = [d for d in diffs
                  if '君主' not in d.split('.')[-1]
                  and '目标城池' not in d.split('.')[-1]]

    if real_diffs:
        print(f"  FAIL: 意外的结构变化:")
        for d in real_diffs:
            print(f"    {d}")
        return False

    print(f"  PASS: 势力修改正常, {len(data['vstates'])} 个势力结构完整")
    return True


def test_settings_modify_structure(filename):
    """测试：修改游戏设置后 JSON 结构不变"""
    print(f"\n{'='*60}")
    print(f"测试: 修改游戏设置 [{filename}]")
    print(f"{'='*60}")

    data, _ = load_save(filename)
    original = copy.deepcopy(data)

    data['year'] = 200
    data['month'] = 12
    if 'game_set' in data:
        data['game_set']['技能系统'] = '是'

    modified_str = serialize(data)
    modified = json.loads(modified_str)

    diffs = deep_compare_keys(original, modified)
    real_diffs = [d for d in diffs
                  if 'year' not in d.split('.')[-1]
                  and 'month' not in d.split('.')[-1]
                  and '技能系统' not in d.split('.')[-1]]

    if real_diffs:
        print(f"  FAIL: 意外的结构变化:")
        for d in real_diffs:
            print(f"    {d}")
        return False

    print(f"  PASS: 设置修改正常")
    return True


def test_empty_field_preserved(filename):
    """测试：空字段不会被删除（模拟 actor.remove 的 bug）"""
    print(f"\n{'='*60}")
    print(f"测试: 空字段保留 [{filename}]")
    print(f"{'='*60}")

    data, _ = load_save(filename)

    # 找一个有 '字' 字段为空字符串的武将
    actor_with_empty = None
    for a in data['actors']:
        if '字' in a and a['字'] == '':
            actor_with_empty = a
            break

    if actor_with_empty is None:
        print("  SKIP: 没有找到空 '字' 字段的武将")
        return True

    # 模拟设置空值但不删除
    actor_with_empty['字'] = ''  # 保留空字符串

    modified_str = serialize(data)
    modified = json.loads(modified_str)

    # 找到同一个武将，检查 '字' 字段还在
    for a in modified['actors']:
        if a['ID'] == actor_with_empty['ID']:
            assert '字' in a, f"FAIL: '字' 字段被删除了!"
            print(f"  PASS: 空字段被保留 (武将{actor_with_empty['姓名']}, 字='{a['字']}')")
            return True

    print("  FAIL: 找不到原武将")
    return False


def test_utf8_preservation(filename):
    """测试：中文字符在 round-trip 后不变"""
    print(f"\n{'='*60}")
    print(f"测试: UTF-8 中文保留 [{filename}]")
    print(f"{'='*60}")

    data, _ = load_save(filename)

    # 收集所有武将名
    names_before = [(a['ID'], a['姓名']) for a in data['actors'] if '姓名' in a]

    modified_str = serialize(data)
    modified = json.loads(modified_str)

    names_after = [(a['ID'], a['姓名']) for a in modified['actors'] if '姓名' in a]

    if names_before != names_after:
        print(f"  FAIL: 中文名称不一致")
        for (id1, n1), (id2, n2) in zip(names_before[:5], names_after[:5]):
            if n1 != n2:
                print(f"    ID={id1}: '{n1}' → '{n2}'")
        return False

    print(f"  PASS: {len(names_before)} 个武将名称全部保留")
    return True


def test_array_field_handling(filename):
    """测试：数组字段(标签)不会被覆盖为字符串"""
    print(f"\n{'='*60}")
    print(f"测试: 数组字段保留 [{filename}]")
    print(f"{'='*60}")

    data, _ = load_save(filename)

    # 找有标签的武将
    actors_with_tags = [(a['ID'], a['姓名'], a['标签'])
                        for a in data['actors'] if '标签' in a]

    if not actors_with_tags:
        print("  SKIP: 没有带标签的武将")
        return True

    print(f"  找到 {len(actors_with_tags)} 个带标签的武将")

    # 修改标签（模拟数组编辑）
    for a in data['actors']:
        if '标签' in a:
            a['标签'] = ['修改标签1', '修改标签2']

    modified_str = serialize(data)
    modified = json.loads(modified_str)

    for a in modified['actors']:
        if '标签' in a:
            tags = a['标签']
            if not isinstance(tags, list):
                print(f"  FAIL: 武将{a['ID']}的标签变成了 {type(tags).__name__} 而不是 list")
                return False

    print(f"  PASS: 标签数组类型保留正确")
    return True


def test_json_valid_encoding(filename):
    """测试：序列化后的 JSON 是有效的 UTF-8"""
    print(f"\n{'='*60}")
    print(f"测试: JSON 编码验证 [{filename}]")
    print(f"{'='*60}")

    data, _ = load_save(filename)

    modified_str = serialize(data)

    # 验证 UTF-8 编码
    try:
        encoded = modified_str.encode('utf-8')
        encoded.decode('utf-8')  # round-trip
    except Exception as e:
        print(f"  FAIL: UTF-8 编码失败: {e}")
        return False

    # 验证 JSON 格式
    try:
        json.loads(modified_str)
    except json.JSONDecodeError as e:
        print(f"  FAIL: JSON 解析失败: {e}")
        return False

    # 验证没有意外的转义
    if '\\\\' in modified_str and '\\\\' not in json.dumps({'test': '\\'}):
        pass  # 可能是正常的

    print(f"  PASS: UTF-8 编码正确, {len(encoded)} bytes")
    return True


def test_equipment_data_preserved(filename):
    """测试：装备数据在 round-trip 后保留"""
    print(f"\n{'='*60}")
    print(f"测试: 装备数据保留 [{filename}]")
    print(f"{'='*60}")

    data, _ = load_save(filename)

    for key in ['weapons', 'steeds', 'suits', 'jewelrys']:
        if key in data:
            count = len(data[key])
            modified_str = serialize(data)
            modified = json.loads(modified_str)
            if key not in modified:
                print(f"  FAIL: '{key}' 丢失了!")
                return False
            if len(modified[key]) != count:
                print(f"  FAIL: '{key}' 数量变化: {count} → {len(modified[key])}")
                return False
            print(f"  {key}: {count} items OK")
        else:
            print(f"  {key}: not present (OK)")

    print(f"  PASS: 装备数据完整")
    return True


def run_all_tests():
    results = []
    for filename in ['7.sav', '4.sav', '2.sav']:
        path = os.path.join(SAVE_DIR, filename)
        if not os.path.exists(path):
            print(f"\n跳过 {filename}: 文件不存在")
            continue

        print(f"\n{'#'*60}")
        print(f"# 测试文件: {filename}")
        print(f"{'#'*60}")

        tests = [
            test_actor_modify_structure,
            test_city_modify_structure,
            test_faction_modify_structure,
            test_settings_modify_structure,
            test_empty_field_preserved,
            test_utf8_preservation,
            test_array_field_handling,
            test_json_valid_encoding,
            test_equipment_data_preserved,
        ]

        for test_func in tests:
            try:
                result = test_func(filename)
                results.append((f"{filename}/{test_func.__name__}", result))
            except Exception as e:
                print(f"  ERROR: {e}")
                import traceback
                traceback.print_exc()
                results.append((f"{filename}/{test_func.__name__}", False))

    # Summary
    print(f"\n{'='*60}")
    print(f"测试结果汇总")
    print(f"{'='*60}")
    passed = sum(1 for _, r in results if r)
    failed = len(results) - passed
    for name, result in results:
        status = "PASS" if result else "FAIL"
        print(f"  [{status}] {name}")
    print(f"\n通过: {passed}/{len(results)}, 失败: {failed}/{len(results)}")
    return failed == 0


if __name__ == '__main__':
    success = run_all_tests()
    sys.exit(0 if success else 1)
