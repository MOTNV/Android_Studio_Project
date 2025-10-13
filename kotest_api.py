from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse
import requests
import json
import re

app = FastAPI(title="Anon Counsel AI Server", version="1.2.0")

# 모델 설정
DEFAULT_MODEL = "anpigon/exaone-3.0-7.8b-instruct-llamafied:latest"
OLLAMA_URL = "http://localhost:11434/api/generate"
REQUEST_TIMEOUT = 60

# --- [1. 웹 테스트 인터페이스 (JSON 디버깅 기능 포함)] ---
@app.get("/", response_class=HTMLResponse)
def read_root():
    return """
    <!DOCTYPE html>
    <html lang="ko">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>익명 상담 AI 라우팅 테스트 (Debug Mode)</title>
        <style>
            body { font-family: 'Apple SD Gothic Neo', sans-serif; background-color: #f4f7f6; display: flex; justify-content: center; padding-top: 30px; padding-bottom: 50px; }
            .container { background: white; width: 90%; max-width: 700px; padding: 30px; border-radius: 15px; box-shadow: 0 10px 25px rgba(0,0,0,0.1); }
            h1 { color: #333; text-align: center; margin-bottom: 20px; }
            textarea { width: 100%; height: 100px; padding: 15px; border: 2px solid #ddd; border-radius: 10px; font-size: 16px; resize: none; box-sizing: border-box; }
            button { width: 100%; padding: 15px; background-color: #0288d1; color: white; border: none; border-radius: 10px; font-size: 18px; font-weight: bold; cursor: pointer; margin-top: 15px; transition: 0.3s; }
            button:hover { background-color: #0277bd; }
            button:disabled { background-color: #ccc; cursor: not-allowed; }
            .result-box { margin-top: 30px; background-color: #f8f9fa; padding: 20px; border-radius: 10px; border-left: 5px solid #0288d1; display: none; }
            .label { font-size: 13px; color: #666; font-weight: bold; margin-bottom: 4px; text-transform: uppercase; }
            .value { font-size: 17px; color: #333; margin-bottom: 12px; font-weight: 500; }
            .highlight { color: #0288d1; font-size: 20px; font-weight: bold; }
            .debug-box { margin-top: 20px; background-color: #2d2d2d; color: #00ff00; padding: 15px; border-radius: 8px; font-family: 'Courier New', monospace; font-size: 14px; overflow-x: auto; display: none; }
            .debug-title { margin-top: 30px; font-size: 14px; color: #999; font-weight: bold; border-bottom: 1px solid #ddd; padding-bottom: 5px; display: none; }
            .loading { text-align: center; display: none; margin-top: 20px; color: #666; font-weight: bold; }
        </style>
    </head>
    <body>
        <div class="container">
            <h1>🤖 AI 라우팅 & JSON 디버그</h1>
            <p style="color: #666; margin-bottom: 10px;">학생 민원 내용을 입력하세요:</p>
            <textarea id="inputInfo" placeholder="예: 교수님, 캡스톤 디자인 팀원 때문에 너무 힘들어서 상담하고 싶습니다."></textarea>
            <button onclick="analyze()" id="btnSubmit">분석 실행 (Analyze)</button>
            <div class="loading" id="loading">🧠 AI(Exaone 3.0)가 분석 중입니다...</div>
            <div class="result-box" id="resultArea">
                <div class="label">추천 수신자</div>
                <div class="value highlight" id="resName"></div>
                <div class="label">이메일 / 위치</div>
                <div class="value">
                    <span id="resEmail"></span> <span style="color:#ccc;">|</span> <span id="resOffice"></span>
                </div>
                <div class="label">분류 (Category / Urgency)</div>
                <div class="value"><span id="resCategory"></span> / <span id="resUrgency" style="color: #e53935; font-weight:bold;"></span></div>
                <div class="label">AI 분석 근거</div>
                <div class="value" id="resReason" style="font-size: 15px; line-height: 1.5; color: #555;"></div>
            </div>
            <div class="debug-title" id="debugTitle">🛠 Server Raw Response (JSON)</div>
            <div class="debug-box" id="debugArea"></div>
        </div>
        <script>
            async function analyze() {
                const text = document.getElementById('inputInfo').value;
                if (!text) { alert("내용을 입력해주세요!"); return; }
                const btn = document.getElementById('btnSubmit');
                const loading = document.getElementById('loading');
                const resultArea = document.getElementById('resultArea');
                const debugArea = document.getElementById('debugArea');
                const debugTitle = document.getElementById('debugTitle');
                btn.disabled = true;
                loading.style.display = 'block';
                resultArea.style.display = 'none';
                debugArea.style.display = 'none';
                debugTitle.style.display = 'none';
                try {
                    const response = await fetch('/analyze', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ content: text }) });
                    const data = await response.json();
                    document.getElementById('resName').innerText = data.recipient_name;
                    document.getElementById('resEmail').innerText = data.recipient_email;
                    document.getElementById('resOffice').innerText = data.recipient_office;
                    document.getElementById('resCategory').innerText = data.category;
                    document.getElementById('resUrgency').innerText = data.urgency;
                    document.getElementById('resReason').innerText = data.reason;
                    resultArea.style.display = 'block';
                    debugArea.innerText = JSON.stringify(data, null, 4);
                    debugArea.style.display = 'block';
                    debugTitle.style.display = 'block';
                } catch (error) {
                    alert("오류가 발생했습니다: " + error);
                    debugArea.innerText = "Error: " + error;
                    debugArea.style.display = 'block';
                } finally {
                    btn.disabled = false;
                    loading.style.display = 'none';
                }
            }
        </script>
    </body>
    </html>
    """

# --- [2. 교수진 DB] ---
PROFESSOR_DB = {
    "정동원": {"field": "데이터베이스, 데이터표준화, 엣지컴퓨팅", "lab": "Information Sciences & Technology Lab", "email": "djeong@kunsan.ac.kr", "office": "디지털정보관 151-106"},
    "온병원": {"field": "데이터 마이닝, 빅데이터, 인공지능, 강화학습", "lab": "Data Intelligence Lab", "email": "bwon@kunsan.ac.kr", "office": "디지털정보관 151-109"},
    "이석훈": {"field": "사물인터넷, 데이터 공학, 시맨틱 웹, 헬스케어", "lab": "Data Semantics Lab", "email": "leha82@kunsan.ac.kr", "office": "디지털정보관 151-108"},
    "손창환": {"field": "컴퓨터 비전, 영상처리, 딥러닝, 기계학습, 그래픽스", "lab": "Computer Vision & Machine Learning Lab", "email": "cson@kunsan.ac.kr", "office": "디지털정보관 151-105"},
    "김장원": {"field": "실시간 빅데이터 처리, 자연어처리(NLP), 지식그래프, 데이터 거버넌스", "lab": "Ambient Human & Machine Intelligence Lab", "email": "jwgim@kunsan.ac.kr", "office": "자연과학대학 4502"},
    "정현준": {"field": "IoT, 블록체인, 네트워크", "lab": "Blockchain Intelligence Lab", "email": "junghj85@kunsan.ac.kr", "office": "디지털정보관 151-228"},
    "김능회": {"field": "소프트웨어공학, 오피니언 마이닝, 빅데이터", "lab": "User and Information Lab", "email": "nunghoi@kunsan.ac.kr", "office": "디지털정보관 151-340"},
    "남영주": {"field": "차량 네트워크, IoT, 인공지능, 최적화", "lab": "Mobility Network Optimization Lab", "email": "imnyj@kunsan.ac.kr", "office": "자연과학대학 4501"},
    "마준": {"field": "그래픽스, 디지털트윈, 게임, AR/VR, 의료 AI", "lab": "Computer Graphics Lab", "email": "junma@kunsan.ac.kr", "office": "디지털정보관 151-118"},
    "학과조교": {"field": "수강신청, 성적문의, 휴학/복학, 장학금, 일반행정, 학사일정", "lab": "학과사무실", "email": "office@kunsan.ac.kr", "office": "학과사무실"}
}
PROFESSOR_CONTEXT_STR = "\n".join([f"- {name}: {info['field']}" for name, info in PROFESSOR_DB.items()])

# --- [3. 데이터 모델] ---
class AnalysisRequest(BaseModel):
    content: str

class RoutingResponse(BaseModel):
    category: str
    urgency: str
    recipient_name: str
    recipient_email: str
    recipient_office: str
    keywords: list[str]
    reason: str

# --- [4. API 로직] ---
def query_ollama(prompt: str, model: str = DEFAULT_MODEL) -> str:
    data = {"model": model, "prompt": prompt, "stream": False, "num_predict": 500, "temperature": 0.1, "format": "json"}
    try:
        response = requests.post(OLLAMA_URL, json=data, timeout=REQUEST_TIMEOUT)
        response.raise_for_status()
        return response.json().get("response", "").strip()
    except Exception as e:
        print(f"❌ Ollama Error: {e}")
        return "{}"

def resolve_professor(name: str):
    key = re.sub(r"(교수|님)$", "", name).strip()
    if key in PROFESSOR_DB:
        return key, PROFESSOR_DB[key]
    # fallback to 조교
    return "학과조교", PROFESSOR_DB["학과조교"]


def build_prompt(user_content: str) -> str:
    return f"""
    [SYSTEM]
    당신은 대학교 학과 민원 자동 분류 시스템입니다.
    학생의 민원 내용을 분석하여 가장 적합한 **수신자**와 **카테고리**를 선택하세요.

    [수신자 목록]
    {PROFESSOR_CONTEXT_STR}

    [카테고리 선택지 (반드시 아래 중 1개 선택)]
    - 수강문의
    - 성적관련
    - 학업상담
    - 개인고민
    - 긴급신고
    - 기타

    [판단 기준]
    1. 연구/진로/랩실 관련 → 해당 교수 선택 (카테고리: 학업상담)
    2. 행정/신고 → 학과조교 선택
       - 신고/폭력 → 긴급신고
       - 수강/성적 → 수강문의/성적관련
    3. 판단 불가 → 학과조교 (카테고리: 기타)

    [User Input]
    {user_content}

    [Output JSON]
    {{
        "category": "긴급신고",
        "urgency": "긴급",
        "recipient_name": "학과조교",
        "keywords": ["논문", "표절", "강압"],
        "reason": "..."
    }}
    """


def safe_json_load(raw: str) -> dict:
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        return {}


@app.post("/analyze", response_model=RoutingResponse)
def analyze_complaint(request: AnalysisRequest):
    content = (request.content or "").strip()
    if not content:
        raise HTTPException(status_code=400, detail="내용이 비어 있습니다.")

    prompt = build_prompt(content)
    raw_response = query_ollama(prompt)
    result = safe_json_load(raw_response)

    name = result.get("recipient_name", "학과조교") or "학과조교"
    name, prof_info = resolve_professor(name)
    return RoutingResponse(
        category=result.get("category", "기타"),
        urgency=result.get("urgency", "일반"),
        recipient_name=name,
        recipient_email=prof_info["email"],
        recipient_office=prof_info["office"],
        keywords=result.get("keywords", []),
        reason=result.get("reason", "자동 분석")
    )

@app.get("/professors")
def list_professors():
    return PROFESSOR_DB

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
