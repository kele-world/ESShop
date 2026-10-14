@echo off
chcp 65001 >nul
echo ========================================
echo   ESshop 智能客服服务
echo ========================================

if not exist .env (
    echo [错误] 未找到 .env 文件
    echo 请复制 .env.example 为 .env 并填入 LLM_API_KEY
    pause
    exit /b 1
)

if not exist venv (
    echo [1/3] 创建虚拟环境...
    python -m venv venv
)

echo [2/3] 安装依赖...
call venv\Scripts\activate.bat
pip install -r requirements.txt -q

echo [3/3] 启动服务...
echo 服务地址: http://localhost:8000
echo 接口文档: http://localhost:8000/docs
echo.
python main.py
