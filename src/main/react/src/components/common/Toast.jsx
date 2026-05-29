import { useEffect, useState } from 'react';

export default function Toast({ type = 'info', message, duration = 5000, onClose }) {
  const [visible, setVisible] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      setVisible(false);
      setTimeout(onClose, 300); // Wait for fade animation
    }, duration);

    return () => clearTimeout(timer);
  }, [duration, onClose]);

  const variants = {
    success: { bg: 'bg-success', icon: 'bi-check-circle' },
    error: { bg: 'bg-danger', icon: 'bi-exclamation-triangle' },
    warning: { bg: 'bg-warning', icon: 'bi-exclamation-circle', textColor: 'text-dark' },
    info: { bg: 'bg-info', icon: 'bi-info-circle' },
  };

  const variant = variants[type] || variants.info;

  return (
    <div className="toast-container">
      <div 
        className={`toast show ${visible ? 'fade-in' : 'fade-out'}`}
        role="alert" 
        aria-live="assertive" 
        aria-atomic="true"
      >
        <div className={`toast-header ${variant.bg} text-white ${variant.textColor || ''}`}>
          <i className={`bi ${variant.icon} me-2`}></i>
          <strong className="me-auto">
            {type.charAt(0).toUpperCase() + type.slice(1)}
          </strong>
          <button 
            type="button" 
            className="btn-close btn-close-white" 
            aria-label="Close"
            onClick={() => {
              setVisible(false);
              setTimeout(onClose, 300);
            }}
          ></button>
        </div>
        <div className="toast-body">
          {message}
        </div>
      </div>
      
      <style>{`
        .fade-in {
          animation: fadeIn 0.3s ease-in;
        }
        .fade-out {
          animation: fadeOut 0.3s ease-out;
        }
        @keyframes fadeIn {
          from { opacity: 0; transform: translateY(-10px); }
          to { opacity: 1; transform: translateY(0); }
        }
        @keyframes fadeOut {
          from { opacity: 1; transform: translateY(0); }
          to { opacity: 0; transform: translateY(-10px); }
        }
      `}</style>
    </div>
  );
}
