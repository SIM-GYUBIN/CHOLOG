interface DashboardIconProps {
    color?: string;
    width?: string | number;
    height?: string | number;
    className?: string;
  }
  
  export default function DashboardIcon({ 
    color = 'currentColor',
    width = 24,
    height = 24,
    className
  }: DashboardIconProps) {
    return (
      <svg 
        width={width} 
        height={height} 
        viewBox="0 0 24 24" 
        fill="none" 
        xmlns="http://www.w3.org/2000/svg"
        className={className}
      >
        <path d="..." fill={color}/>
      </svg>
    );
  }